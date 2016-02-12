package nettest;


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

import netpacket.DiscoverPacket;
import netpacket.GoalDistanceMessage;
import netpacket.NetworkMessage;

/**
 *
 */
public class Jetson {
    
    // Put methods for controlling this subsystem
    // here. Call these from Commands.
	
	final static int remotePort = 5800;
	
	private InterfaceAddress ifaddr;
	private Socket connection;
	private DatagramSocket udpSocket;
	private OutputStream netOut;
	private InputStream netIn;
    
	public InetAddress getJetsonAddress() {
		if(connection == null) {
			return null;
		}
		
		return connection.getInetAddress();
	}
	
    public Jetson() throws UnknownHostException, IOException {
    	Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    	NetworkInterface ifn = null;
    	
    	for(NetworkInterface iface : Collections.list(interfaces)) {
    		System.out.println("Interfaces: " + iface.getName());
    		if(iface.getName().equals(new String("eth0")) && iface.isUp() && !iface.isLoopback() && !iface.isVirtual()) {
    			ifn = iface;
    			break;
    		}
    	}
    	
    	if(ifn == null) {
    		System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
    		return;
    	}
    	
    	List<InterfaceAddress> ifnaddr = ifn.getInterfaceAddresses();
		for(InterfaceAddress i : ifnaddr) {
			System.out.println("Address: " + i.toString());
			if(i.getBroadcast() != null) {
				ifaddr = i;
				break;
			}
		}
    	
    	udpSocket = new DatagramSocket(remotePort, ifaddr.getAddress());
    	udpSocket.setBroadcast(true);
    	
    	connection = null;
    	netOut = null;
    	netIn = null;
    }
    
    public void doDiscover() throws IOException {
    	while(true) {
    		this.sendUDP(new DiscoverPacket(ifaddr.getBroadcast()));
    		System.out.println("Sent to: " + ifaddr.getBroadcast().toString());
    		NetworkMessage msg = this.receiveUDP();
    		if(msg instanceof DiscoverPacket) {
    			DiscoverPacket dmsg = (DiscoverPacket)msg;
    			if(dmsg.originator == DiscoverPacket.origin_type.JETSON) {
    				connection = new Socket(msg.addr, remotePort);
    				netOut = connection.getOutputStream();
    		    	netIn = connection.getInputStream();
    		    	System.out.println("Found Jetson at: " + msg.addr.toString());
    		    	return;
    			}
    		}
    	}
    }
    
    public void sendUDP(NetworkMessage msg) throws IOException {
    	ByteBuffer ns = ByteBuffer.allocate(4096);
    	ns.order(ByteOrder.BIG_ENDIAN);
    	
    	ns.put((byte)0x35);
    	ns.put((byte)0x30);
    	ns.put((byte)0x30);
    	ns.put((byte)0x32);
    	
    	ns.put((byte)msg.getMessageID());
    	
    	short size = msg.getMessageSize();
    	
    	ns.putShort((short) (size+7));
    	
    	msg.writeObjectTo(ns);
    	
    	DatagramPacket outpacket = new DatagramPacket(ns.array(), 4096, msg.addr, remotePort);
    	
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
    	
    	System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from " +
    					packet.getAddress().toString() + " length: " + Integer.toString(packet.getLength()));
    	
    	if((buf.get() == (byte)0x35) &&
    		(buf.get() == (byte)0x30) &&
    		(buf.get() == (byte)0x30) &&
    		(buf.get() == (byte)0x32)) { // '5' '0' '0' '2' in true ASCII
    		
    		byte msgType = buf.get();
    		short size = buf.getShort();
    		
    		System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");
    		
    		switch(msgType) {
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
    	if(connection == null) {
    		this.doDiscover();
    	}
    	
    	ByteBuffer ns = ByteBuffer.allocate(4096);
    	ns.order(ByteOrder.BIG_ENDIAN);
    	
    	ns.put((byte)0x35);
    	ns.put((byte)0x30);
    	ns.put((byte)0x30);
    	ns.put((byte)0x32);
    	
    	ns.put((byte)msg.getMessageID());
    	
    	short size = msg.getMessageSize();
    	
    	ns.putShort((short) (size+7));
    	
    	msg.writeObjectTo(ns);
    	
    	netOut.write(ns.array(), 0, 4096);
    }
    
    public NetworkMessage readMessage() throws IOException {
    	if(connection == null) {
    		this.doDiscover();
    	}
    	
    	while(true) {
    		ByteBuffer buf = ByteBuffer.allocate(4096);
    		buf.order(ByteOrder.BIG_ENDIAN);
        	byte[] arr = buf.array();
	    	netIn.read(arr);
	    	
	    	if(buf.get() == (byte)0x35 &&
    			buf.get() == (byte)0x30 &&
				buf.get() == (byte)0x30 &&
				buf.get() == (byte)0x32) {
	    		
	    		byte msgType = buf.get();
	    		short size = buf.getShort();
	    		
	    		switch(msgType) {
	    		case 4:
	    			GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
	    			out.readObjectFrom(buf);
	    			return out;
	    		default:
	    			continue;
	    		}
	    	}
    	}
    }
}

