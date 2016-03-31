package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!isDaijoubu())
			throw IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!isDaijoubu())
			throw IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!isDaijoubu())
			throw IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
				+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
				+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException {
		BlockingQueue<NetworkMessage> oQ = outboundQueue;
		oQ.put(msg);
	}

	private void synSend(NetworkMessage msg) throws IOException {
		if (connection == null) {
			this.doDiscover();
		}

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = inboundQueue;
		return iQ.take();
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage pollMessage() throws IOException {
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException {
		if (connection == null) {
			this.doDiscover();
		}

		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}

		return null;
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public synchronized void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = (GoalDistanceMessage) m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
		//		+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
		//		+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		try {
			oQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void synSend(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
		try {
			return iQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 * @throws IllegalStateException if not connected to the Jetson yet.
	 */
	public NetworkMessage pollMessage() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				//System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public synchronized void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = (GoalDistanceMessage) m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
		//		+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
		//		+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		try {
			oQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void synSend(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
		try {
			return iQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 * @throws IllegalStateException if not connected to the Jetson yet.
	 */
	public NetworkMessage pollMessage() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				//System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public synchronized void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = (GoalDistanceMessage) m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
		//		+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
		//		+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		try {
			oQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void synSend(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
		try {
			return iQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 * @throws IllegalStateException if not connected to the Jetson yet.
	 */
	public NetworkMessage pollMessage() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				//System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public synchronized void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = (GoalDistanceMessage) m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
		//		+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
		//		+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		try {
			oQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void synSend(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
		try {
			return iQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 * @throws IllegalStateException if not connected to the Jetson yet.
	 */
	public NetworkMessage pollMessage() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				//System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public synchronized void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = (GoalDistanceMessage) m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
		//		+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
		//		+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		try {
			oQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void synSend(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
		try {
			return iQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 * @throws IllegalStateException if not connected to the Jetson yet.
	 */
	public NetworkMessage pollMessage() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				//System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public synchronized void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = (GoalDistanceMessage) m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
		//		+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
		//		+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		try {
			oQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void synSend(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
		try {
			return iQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 * @throws IllegalStateException if not connected to the Jetson yet.
	 */
	public NetworkMessage pollMessage() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				//System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public synchronized void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = (GoalDistanceMessage) m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
		//		+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
		//		+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		try {
			oQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void synSend(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
		try {
			return iQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 * @throws IllegalStateException if not connected to the Jetson yet.
	 */
	public NetworkMessage pollMessage() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				//System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public synchronized void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = (GoalDistanceMessage) m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
		//		+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
		//		+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		try {
			oQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void synSend(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
		try {
			return iQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 * @throws IllegalStateException if not connected to the Jetson yet.
	 */
	public NetworkMessage pollMessage() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				//System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public synchronized void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = (GoalDistanceMessage) m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
		//		+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
		//		+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		try {
			oQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void synSend(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
		try {
			return iQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 * @throws IllegalStateException if not connected to the Jetson yet.
	 */
	public NetworkMessage pollMessage() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				//System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
package org.usfirst.frc.team5002.robot.subsystems;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 5801;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InetAddress remoteAddr;
	private InterfaceAddress ifaddr;

	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;

	private Thread camThread;
	private boolean camThreadStopStatus = false;

	private Thread recvThread;
	private Thread sendThread;

	private Queue<NetworkMessage> outboundQueue;
	private Queue<NetworkMessage> inboundQueue;

	private boolean lastKnownGoalStatus = false;
	private double lastKnownDistance = 0.0;
	private double lastKnownAngle = 0.0;

	public synchronized void updateSD() {
		SmartDashboard.putBoolean("jetson.located", (remoteAddr != null));
		SmartDashboard.putBoolean("jetson.connected", (connection != null));

		SmartDashboard.putString("jetson.localAddress", ifaddr.toString());

		if(remoteAddr != null) {
			SmartDashboard.putString("jetson.remoteAddress", remoteAddr.toString());
		} else {
			SmartDashboard.putString("jetson.remoteAddress", "unknown");
		}

		SmartDashboard.putBoolean("jetson.goal.found", lastKnownGoalStatus);
		SmartDashboard.putNumber("jetson.goal.distance", lastKnownDistance);
		SmartDashboard.putNumber("jetson.goal.angle", lastKnownAngle);
	}

	private void jetsonRecvThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		NetworkMessage m = synRecv();

		/* TODO: This is game-specific code and should ideally be separated into its own class.
		 * 	However, at this point it would be too much work to separate the general network code from everything else.
		 */
		if(m instanceof GoalDistanceMessage) {
			GoalDistanceMessage gdm = (GoalDistanceMessage) m;
			synchronized(this) {
				lastKnownGoalStatus = (gdm.goal_status == GoalDistanceMessage.Status.GOAL_FOUND);
				if(lastKnownGoalStatus) {
					lastKnownDistance = gdm.distance;
					lastKnownAngle = gdm.angle;
				}
			}
		} else {
			BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
			iQ.put(m);
		}
	}

	private void jetsonSendThread() throws IOException, InterruptedException {
		while(connection == null) {
			synchronized(this) {
				this.wait();
			}
		}

		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		while(true) {
			NetworkMessage m = oQ.take();

			synSend(m);
		}
	}

	/***
	 * Get Jetson connection status.
	 *
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}

	/**
	 * Get last received goal state from the Jetson.
	 *
	 * @return whether the Jetson can detect the goal or not
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized boolean getGoalStatus() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownGoalStatus;
	}

	/**
	 * Get last received goal distance from the Jetson.
	 *
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getDistance() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownDistance;
	}

	/**
	 * Get last received angle off goal centerline from the Jetson.
	 *
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public synchronized double getAngle() throws IllegalStateException {
		if(!this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		return lastKnownAngle;
	}

	/**
	 * Set default command. Does nothing currently.
	 */
	public void initDefaultCommand() {
	}

	/**
	 * Get the LAN IP address of the Jetson.
	 * @return the Jetson's IP address.
	 */
	public InetAddress getJetsonAddress() {
		return remoteAddr;
	}

	/**
	 * Attempts to initialize network resources required to find and communicate with the Jetson.
	 *
	 * @throws IOException in the event of network errors (mostly passed up the network stack).
	 */
	public Jetson() throws IOException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		NetworkInterface ifn = null;
		for (NetworkInterface iface : Collections.list(interfaces)) {
			System.out.println("Interfaces: " + iface.getName());
			if (iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback()
					&& !iface.isVirtual()) {
				ifn = iface;
				break;
			}
		}

		if (ifn == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
			return;
		}

		List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for (InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if (i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}

		udpSocket = new DatagramSocket(remotePort);
		udpSocket.setBroadcast(true);

		inboundQueue = new LinkedList<NetworkMessage>();
		outboundQueue = new LinkedList<NetworkMessage>();
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
		while (true) {
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					remoteAddr = msg.addr;
					return;
				}
			}
		}
	}

	/**
	 * Connect to the Jetson RPC server and start the send/receive threads.
	 * @throws IOException in the event of network errors.
	 */
	public void initMainStream() throws IOException {
		connection = new Socket(remoteAddr, remotePort);
		netOut = connection.getOutputStream();
		netIn = connection.getInputStream();

		recvThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonRecvThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		sendThread = new Thread(new Runnable() {
				public void run() {
					try {
						jetsonSendThread();
					} catch(IOException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		);

		recvThread.setName("Jetson Receive Thread");
		recvThread.start();

		sendThread.setName("Jetson Send Thread");
		sendThread.start();
	}

	/**
	 * Send a network message to the Jetson over UDP.
	 *
	 * Due to the packet size limitations of UDP, use of this function should be limited to small, simple packets.
	 *
	 * @param msg Network message type to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		DatagramPacket outpacket = new DatagramPacket(ns.array(), size + 7, msg.addr, remotePort);

		udpSocket.send(outpacket);
	}

	/**
	 * Synchronously receives a network message over UDP.
	 * This operation blocks.
	 *
	 * @return Received network message
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage receiveUDP() throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4096);
		buf.order(ByteOrder.BIG_ENDIAN);
		byte[] arr = buf.array();
		DatagramPacket packet = new DatagramPacket(arr, 4096);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
		//		+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
		//		+ packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));

		if ((buf.get() == (byte) 0x35) && (buf.get() == (byte) 0x30) && (buf.get() == (byte) 0x30)
				&& (buf.get() == (byte) 0x32)) { // '5' '0' '0' '2' in true
													// ASCII

			byte msgType = buf.get();
			short size = buf.getShort();

			//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");

			switch (msgType) {
			case 5:
				DiscoverPacket out = new DiscoverPacket(packet.getAddress());
				out.readObjectFrom(buf);
				return out;
			default:
				//System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		}/* else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}*/
		return null;
	}

	/**
	 * Sends a network message to the Jetson over TCP.
	 * As it communicates to the TCP server on the Jetson, this requires the discovery protocol to be run first.
	 * See doDiscover() for more information.
	 *
	 * @param msg message to send
	 * @throws IOException in the event of network errors.
	 */
	public void sendMessage(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		BlockingQueue<NetworkMessage> oQ = (BlockingQueue<NetworkMessage>) outboundQueue;
		try {
			oQ.put(msg);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void synSend(NetworkMessage msg) throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");

		ByteBuffer ns = ByteBuffer.allocate(msg.getMessageSize()+7);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, msg.getMessageSize()+7);
		netOut.flush();
	}

	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 *
	 * @throws IOException in the event of network errors.
	 */
	public NetworkMessage readMessage() throws IOException {
		BlockingQueue<NetworkMessage> iQ = (BlockingQueue<NetworkMessage>) inboundQueue;
		try {
			return iQ.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Asynchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will not block, but can return null.
	 *
	 * @return NetworkMessage containing the last received network message.
	 * @throws IOException in the event of network errors.
	 * @throws IllegalStateException if not connected to the Jetson yet.
	 */
	public NetworkMessage pollMessage() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		synchronized(inboundQueue) {
			return inboundQueue.poll();
		}
	}

	private NetworkMessage synRecv() throws IOException, IllegalStateException {
		if(connection == null || !this.isDaijoubu())
			throw new IllegalStateException("Not connected to Jetson yet!");
		
		while (true) {
			ByteBuffer headerBuf = ByteBuffer.allocate(7);
			headerBuf.order(ByteOrder.BIG_ENDIAN);
			byte[] arr = headerBuf.array();
			int currentPos = 0;
			while(currentPos < 7) {
				int nRead = netIn.read(arr, currentPos, 7-currentPos);
				currentPos += nRead;
			}

			if (headerBuf.get() == (byte) 0x35 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x30 &&
				headerBuf.get() == (byte) 0x32) {

				byte msgType = headerBuf.get();
				short size = headerBuf.getShort();

				ByteBuffer fullBuf = ByteBuffer.allocate(7+size);
				fullBuf.put(arr, 0, 7);

				currentPos = 7;
				int nTotalRead = 0;
				arr = fullBuf.array();
				while(nTotalRead < size) {
					int nRead = netIn.read(arr, currentPos, size - nTotalRead);
					currentPos += nRead;
					nTotalRead += nRead;
				}

				switch (msgType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(fullBuf);
					return out;
				default:
					continue;
				}
			}
		}
	}

	public void initCameraStream(String camName) {
		USBCamera cam = new USBCamera(camName);
		camThread = new Thread(new Runnable() {
				public void run() {
					try {
						miniCameraClient(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);

		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
	}

	public void stopCameraStream() {
		if(camThread == null)
			return;
		camThreadStopStatus = true;
		camThread.interrupt();
	}

	/**
	 * Implements a camera streaming CLIENT (connects to a stream recv server on the Jetson)
	 * @param camera - camera object to stream
	 * @throws IOException in event of network errors
	 * @throws IllegalStateException if not connected to Jetson yet (see DoDiscover)
	 */
	private void miniCameraClient(USBCamera camera) throws IOException, IllegalStateException {
		camera.openCamera();

	    	Socket connSock = new Socket(remoteAddr, cameraRemotePort); //listenSocket.accept();

	    	DataInputStream in = new DataInputStream(connSock.getInputStream());
	    	DataOutputStream out = new DataOutputStream(connSock.getOutputStream());

	    	int fps = in.readInt();
	    	in.readInt(); // compression is irrelevant
	    	int size = in.readInt();

	    	switch(size) {
	    	case 0:	// 640 x 480
	    		camera.setSize(640, 480);
	    		break;
	    	case 1: // 320 x 240
	    		camera.setSize(320, 240);
	    		break;
	    	case 2:	// 160 x 120
	    		camera.setSize(160, 120);
	    		break;
	    	}


	    	long period = (long) (1000 / (1.0 * fps));
	    	long loopTime = System.currentTimeMillis();

	    	camera.updateSettings();
	    	camera.startCapture();

	    	while(true) {
				// capture loop
				loopTime = System.currentTimeMillis();
				/*
				Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
				camera.getImage(frame);

				RawData data =
				    NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
				        NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
				ByteBuffer buf = data.getBuffer();
				*/

				if(camThreadStopStatus) {
					return;
				}

				ByteBuffer buf = ByteBuffer.allocateDirect(200000); // just get a really big buffer.
				camera.getImageData(buf); // just get JPEG data


				int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}


				buf.position(dataStart);

				//System.out.println("Got " + String.valueOf(buf.remaining()) + " bytes from camera.");

				byte[] payload = new byte[buf.remaining()];
				buf.get(payload, 0, buf.remaining());

				out.write(cameraHeader);
				out.writeInt(payload.length);
				out.write(payload);
				out.flush();

				long timeDelta = (System.currentTimeMillis() - loopTime);

				if(timeDelta < period) {
					try {
						Thread.sleep(period - timeDelta);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	    	}
	}
}
