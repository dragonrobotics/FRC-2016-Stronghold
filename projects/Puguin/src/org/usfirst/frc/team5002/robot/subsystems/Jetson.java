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

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.vision.USBCamera;

import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.StartVideoStream;

import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import com.ni.vision.NIVision.RawData;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;
	final static int cameraRemotePort = 1180;
	final static byte[] cameraHeader = {0x01, 0x00, 0x00, 0x00};

	private InterfaceAddress ifaddr;
	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;
	
	private Thread camThread;
	
	private enum JetsonStateMachine {
		READ_HEADER,	// reading packet header
		READ_DATA,		// reading packet payload
		WAIT			// not reading packet
	};
	
	private JetsonStateMachine curState = JetsonStateMachine.WAIT;
	private ByteBuffer curHeaderBuf;
	private ByteBuffer curPacketBuf;
	private int curBufPos;
	private short curPacketSize;
	private byte curPacketType;
	
	private Queue<NetworkMessage> msgQueue;
	
	private double lastKnownDistance;
	private double lastKnownAngle;
	
	/**
	 * Process queued goal state messages from the Jetson.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	private void updateGoalStatus() throws IllegalStateException {
		if(!connection.isConnected()) {
			throw new IllegalStateException("Not connected to Jetson yet.");
		} else {
			NetworkMessage i;
			while(!msgQueue.isEmpty()) {
				i = msgQueue.poll();
				if((i != null) && (i instanceof GoalDistanceMessage)) {
					GoalDistanceMessage m = (GoalDistanceMessage)i;
					lastKnownDistance = m.distance;
					lastKnownAngle = m.angle;
				}
			}
		}
	}
	
	/***
	 * Get Jetson sortie status.
	 * 
	 * @return is the Jetson connected or not?
	 */
	public boolean isDaijoubu() {
		return (connection.isConnected());
	}
	
	/**
	 * Get last received goal distance from the Jetson.
	 * 
	 * @return how far the camera / robot is from the goal
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public double getDistance() throws IllegalStateException {
		updateGoalStatus();
		return lastKnownDistance;
	}
	
	/**
	 * Get last received angle off goal centerline from the Jetson.
	 * 
	 * @return approximate angle off goal target.
	 * @throws IllegalStateException if a connection to the Jetson could not be established.
	 */
	public double getAngle() throws IllegalStateException {
		updateGoalStatus();
		return lastKnownAngle;
	}
	
	/**
	 *  Reset the internal asynchronous receive buffers and state machine.
	 */
	private void resetAsyncState() {
		curState = JetsonStateMachine.WAIT;
		curHeaderBuf = null;
		curPacketBuf = null;
		curBufPos = 0;
		curPacketSize = 0;
		curPacketType = 0;
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
		if (connection == null) {
			return null;
		}

		return connection.getInetAddress();
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
	}

	/**
	 * Attempt to find the Jetson using a UDP discovery protocol.
	 * @throws IOException in the event of network errors.
	 */
	public void doDiscover() throws IOException {
		while (true) {
			// this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
			// System.out.println("Sent to: " +
			// ifaddr.getBroadcast().toString());
			NetworkMessage msg = this.receiveUDP();
			if (msg instanceof DiscoverPacket) {
				DiscoverPacket dmsg = (DiscoverPacket) msg;
				if (dmsg.originator == DiscoverPacket.origin_type.JETSON) {
					System.out.println("Found Jetson at: " + msg.addr.toString());
					connection = new Socket(msg.addr, remotePort);
					netOut = connection.getOutputStream();
					netIn = connection.getInputStream();
					return;
				}
			}
		}
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

		System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
				+ udpSocket.getLocalAddress().toString() + " on " + Integer.toString(udpSocket.getPort()));

		udpSocket.receive(packet);

		System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from "
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
				System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] unknown packet type!");
				return null;
			}
		} else {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is INVALID!");
		}
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
	 * Asynchronously check for received messages or received message fragments.
	 * Received messages will be added to a queue as they are received.
	 * Partially received messages will be added to an internal buffer.
	 * The discovery protocol must be run first (see doDiscover() for more information).
	 * 
	 * @throws IOException in the event of network errors.
	 */
	public void checkForMessage() throws IOException {
		switch(curState) {
		default:
		case WAIT: {
			if(netIn.available() > 0) {
				curHeaderBuf = ByteBuffer.allocate(7);
				curHeaderBuf.order(ByteOrder.BIG_ENDIAN);
				curState = JetsonStateMachine.READ_HEADER;
				curBufPos = 0;
				// fall through to READ_HEADER
			} else {
				break;
			}
		}
		case READ_HEADER:
			if(curBufPos > 7) {
				if (curHeaderBuf.get() == (byte) 0x35 &&
					curHeaderBuf.get() == (byte) 0x30 &&
					curHeaderBuf.get() == (byte) 0x30 &&
					curHeaderBuf.get() == (byte) 0x32) {

						byte msgType = curHeaderBuf.get();
						short size = curHeaderBuf.getShort();
						
						curPacketBuf = ByteBuffer.allocate(7+size);
						
						byte[] arr = curHeaderBuf.array();
						curPacketBuf.put(arr, 0, 7);
						
						curPacketBuf.order(ByteOrder.BIG_ENDIAN);
						
						curBufPos = 0;
						curPacketSize  = size;
						curPacketType = msgType;
						
						curState = JetsonStateMachine.READ_DATA;
						// fall through to READ_DATA
				} else {
					resetAsyncState(); // go back to WAIT state
					break;
				}
			}
			
			if(netIn.available() > 0) {
				byte[] arr = curHeaderBuf.array();
				int nRead = netIn.read(arr, curBufPos, 7-curBufPos);
				curBufPos += nRead;
			} else {
				break;
			}
			
		case READ_DATA:
			if(netIn.available() > 0) {
				byte[] arr = curPacketBuf.array();
				int nRead = netIn.read(arr, 7+curBufPos, curPacketSize-curBufPos);
				curBufPos += nRead;
			} else {
				break;
			}
			
			if(curBufPos > curPacketSize) {
				switch (curPacketType) {
				case 4:
					GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
					out.readObjectFrom(curPacketBuf);
					
					msgQueue.add(out);
					
					//return out;
				default:
					break;
				}
				
				resetAsyncState(); // go back to WAIT state
			}
			
			break;
		}
	}
	
	/**
	 * Synchronously waits for a message from the Jetson over TCP.
	 * As this requires a connection to the Jetson, the discovery protocol must be run first (see doDiscover()) for more information.
	 * This operation will block.
	 * 
	 * @throws IOException in the event of network errors.
	 */
	public void readMessage() throws IOException {
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
					
					msgQueue.add(out);
					
					//return out;
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
						miniCameraServer(cam);
					} catch(IOException e) {
						e.printStackTrace();
					}
				}
			}
		);
		
		camThread.setName("Rio->Jetson Camera Stream");
		camThread.start();
		
		try {
			sendMessage(new StartVideoStream(this.connection.getInetAddress()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Re-implements the WPILib camera server class for the Jetson.
	 * @param camera
	 * @throws IOException 
	 */
	private void miniCameraServer(USBCamera camera) throws IOException {
		ServerSocket listenSocket = new ServerSocket();
	    listenSocket.setReuseAddress(true);
	    InetSocketAddress address = new InetSocketAddress(cameraRemotePort);
	    listenSocket.bind(address);
	    
	    while(true) {
	    	// wait for connection
	    	Socket connSock = listenSocket.accept();
	    	
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
	    	
	    	while(true) {
	    		// capture loop
	    		loopTime = System.currentTimeMillis();
	    		
		    	Image frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
		    	camera.getImage(frame);
		    	
		    	RawData data =
		    	        NIVision.imaqFlatten(frame, NIVision.FlattenType.FLATTEN_IMAGE,
		    	            NIVision.CompressionType.COMPRESSION_JPEG, 10 * 50);
		    	ByteBuffer buf = data.getBuffer();
		    	
		    	int dataStart = 0;
				while (dataStart < buf.limit() - 1) {
					if ((buf.get(dataStart) & 0xff) == 0xFF && (buf.get(dataStart + 1) & 0xff) == 0xD8)
					  break;
					dataStart++;
				}
				
				buf.position(dataStart);
				
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
}
