package org.usfirst.frc.team5002.robot.subsystems;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.usfirst.frc.team5002.robot.subsystems.network.NetworkMessage;

import edu.wpi.first.wpilibj.command.Subsystem;

/**
 *
 */
public class Jetson extends Subsystem {
    
    // Put methods for controlling this subsystem
    // here. Call these from Commands.
	
	final static String remoteHost = "derp"; // TODO: Put the IP of the Jetson here
	final static int remotePort = 5800;
	
	private Socket connection;
	private OutputStream netOut;
	private InputStream netIn;
	
	
    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand(new MySpecialCommand());
    }
    
    public Jetson() throws UnknownHostException, IOException {
    	connection = new Socket(remoteHost, remotePort);
    	netOut = connection.getOutputStream();
    	netIn = connection.getInputStream();
    }
    
    public void sendMessage(NetworkMessage msg) throws IOException {
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
    
    public NetworkMessage readMessage() throws IOException, ClassNotFoundException {
    	byte[] buf = new byte[4096];
    	int len = netIn.read();
    	
    	if(buf[0] == (int)'5' &&
    		buf[1] == (int)'0' &&
    		buf[2] == (int)'0' &&
    		buf[3] == (int)'2') {
    		
    		int msgType = buf[4];
    		short size = (short) ((buf[5] << 8) | buf[6]);
    		
    		ObjectInputStream o = new ObjectInputStream(new ByteArrayInputStream(buf, 7, size));
    		
    		switch(msgType) {
    		case 1:
    			return GoalDistanceMessage.readObjectFrom(o);
    		}
    	}
    	
    	return null;
    }
}

