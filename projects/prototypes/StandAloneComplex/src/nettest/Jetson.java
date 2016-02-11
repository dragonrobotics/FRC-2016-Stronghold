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
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;

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
    	udpSocket = new DatagramSocket(remotePort);
    	connection = null;
    	netOut = null;
    	netIn = null;
    }
    
    public void doDiscover() throws IOException {
    	Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    	NetworkInterface ifn = null;
    	
    	for(NetworkInterface iface : Collections.list(interfaces)) {
    		if(iface.isUp() && !iface.isLoopback() && !iface.isVirtual()) {
    			ifn = iface;
    			break;
    		}
    	}
    	
    	if(ifn == null) {
    		System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find usable interface.");
    		return;
    	}
    
    	InetAddress broadcast = null;
    	
    	Enumeration<InetAddress> addresses = ifn.getInetAddresses();
		for (InetAddress address : Collections.list(addresses)) {
		    // look only for ipv4 addresses
			if (address instanceof Inet6Address)
			  continue;
			broadcast = address;
		}
		
		if(broadcast == null) {
			System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Could not find interface broadcast address.");
			return;
		}
    	
    	while(true) {
    		this.sendUDP(new DiscoverPacket(broadcast));
    		NetworkMessage msg = this.receiveUDP();
    		if(msg instanceof DiscoverPacket) {
    			DiscoverPacket dmsg = (DiscoverPacket)msg;
    			if(dmsg.originator == DiscoverPacket.origin_type.JETSON) {
    				connection = new Socket(msg.addr, remotePort);
    				netOut = connection.getOutputStream();
    		    	netIn = connection.getInputStream();
    		    	return;
    			}
    		}
    	}
    }
    
    public void sendUDP(NetworkMessage msg) throws IOException {
    	ByteArrayOutputStream ns = new ByteArrayOutputStream();
    	ByteArrayOutputStream ts = new ByteArrayOutputStream();
    	ObjectOutputStream os = new ObjectOutputStream(ts);
    	
    	ns.write((int)'5');
    	ns.write((int)'0');
    	ns.write((int)'0');
    	ns.write((int)'2');
    	
    	ns.write((int)msg.msgType);
    	
    	short size = (short)(ts.size());
    	
    	ns.write((size >> 8) & 0xFF);
    	ns.write(size & 0xFF);
    	
    	os.writeObject(msg);
    	
    	ts.writeTo(ns);
    	
    	DatagramPacket outpacket = new DatagramPacket(ns.toByteArray(), ns.size(), msg.addr, remotePort);
    	
    	udpSocket.send(outpacket);
    }
    
    public NetworkMessage receiveUDP() throws IOException {
    	byte[] buf = new byte[4096];
    	DatagramPacket packet = new DatagramPacket(buf, 4096);
    	
    	System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Listening on "
    			+ udpSocket.getLocalAddress().toString());
    	
    	udpSocket.receive(packet);
    	
    	System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Received UDP message from " +
    					udpSocket.getInetAddress().toString());
    	
    	if(buf[0] == (int)'5' &&
    		buf[1] == (int)'0' &&
    		buf[2] == (int)'0' &&
    		buf[3] == (int)'2') {
    		
    		int msgType = buf[4];
    		short size = (short) ((buf[5] << 8) | buf[6]);
    		
    		ObjectInputStream o = new ObjectInputStream(new ByteArrayInputStream(buf, 7, size));
    		
    		System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] UDP packet is valid.");
    		
    		switch(msgType) {
    		case 5:
    			DiscoverPacket out = new DiscoverPacket(packet.getAddress());
    			out.readObjectFrom(o);
    			return out;
    		default:
    			return null;
    		}
    	}
    	return null;
    }
    
    public void sendMessage(NetworkMessage msg) throws IOException {
    	if(connection == null) {
    		this.doDiscover();
    	}
    	
    	ByteArrayOutputStream ns = new ByteArrayOutputStream();
    	ByteArrayOutputStream ts = new ByteArrayOutputStream();
    	ObjectOutputStream os = new ObjectOutputStream(ts);
    	
    	ns.write((int)'5');
    	ns.write((int)'0');
    	ns.write((int)'0');
    	ns.write((int)'2');
    	
    	ns.write((int)msg.msgType);
    	
    	short size = (short)(ts.size());
    	
    	ns.write((size >> 8) & 0xFF);
    	ns.write(size & 0xFF);
    	
    	os.writeObject(msg);
    	
    	ts.writeTo(ns);
    	ns.writeTo(netOut);
    }
    
    public NetworkMessage readMessage() throws IOException {
    	if(connection == null) {
    		this.doDiscover();
    	}
    	
    	while(true) {
	    	byte[] buf = new byte[4096];
	    	netIn.read(buf);
	    	
	    	if(buf[0] == (int)'5' &&
	    		buf[1] == (int)'0' &&
	    		buf[2] == (int)'0' &&
	    		buf[3] == (int)'2') {
	    		
	    		int msgType = buf[4];
	    		short size = (short) ((buf[5] << 8) | buf[6]);
	    		
	    		ObjectInputStream o = new ObjectInputStream(new ByteArrayInputStream(buf, 7, 7+size));
	    		
	    		switch(msgType) {
	    		case 4:
	    			GoalDistanceMessage out = new GoalDistanceMessage(connection.getInetAddress());
	    			out.readObjectFrom(o);
	    			return out;
	    		default:
	    			continue;
	    		}
	    	}
    	}
    }
}

