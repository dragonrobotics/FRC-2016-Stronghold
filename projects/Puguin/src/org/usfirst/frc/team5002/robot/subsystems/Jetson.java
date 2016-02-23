package org.usfirst.frc.team5002.robot.subsystems;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;

import edu.wpi.first.wpilibj.command.Subsystem;
import org.usfirst.frc.team5002.robot.subsystems.network.DiscoverPacket;
import org.usfirst.frc.team5002.robot.subsystems.network.GoalDistanceMessage;
import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;

/**
 *
 */
public class Jetson extends Subsystem {
	final static int remotePort = 5800;

	private InterfaceAddress ifaddr;
	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;
	
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
	
	public double getDistance() throws IllegalStateException {
		updateGoalStatus();
		return lastKnownDistance;
	}
	
	public double getAngle() throws IllegalStateException {
		updateGoalStatus();
		return lastKnownAngle;
	}
	
	private void resetAsyncState() {
		curState = JetsonStateMachine.WAIT;
		curHeaderBuf = null;
		curPacketBuf = null;
		curBufPos = 0;
		curPacketSize = 0;
		curPacketType = 0;
	}
	
	public void initDefaultCommand() {
	}

	public InetAddress getJetsonAddress() {
		if (connection == null) {
			return null;
		}

		return connection.getInetAddress();
	}

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

	public void sendUDP(NetworkMessage msg) throws IOException {
		ByteBuffer ns = ByteBuffer.allocate(4096);
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

	public void sendMessage(NetworkMessage msg) throws IOException {
		if (connection == null) {
			this.doDiscover();
		}

		ByteBuffer ns = ByteBuffer.allocate(4096);
		ns.order(ByteOrder.BIG_ENDIAN);

		ns.put((byte) 0x35);
		ns.put((byte) 0x30);
		ns.put((byte) 0x30);
		ns.put((byte) 0x32);

		ns.put((byte) msg.getMessageID());

		short size = msg.getMessageSize();

		ns.putShort((short) (size));

		msg.writeObjectTo(ns);

		netOut.write(ns.array(), 0, 4096);
	}

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
}
