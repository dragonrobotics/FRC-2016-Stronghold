package nettest;

import java.io.IOException;

import netpacket.GoalDistanceMessage;
import netpacket.NetworkMessage;

public class NetTestMain {

	public static void main(String[] args) {
		try {
			Jetson jetson = new Jetson();
			jetson.doDiscover();
			while(true) {
				NetworkMessage msg = jetson.readMessage();
				if(msg instanceof GoalDistanceMessage) {
					GoalDistanceMessage gdm = (GoalDistanceMessage)msg;
					System.out.println("[" + Long.toString(System.currentTimeMillis()) + "] Got goal distance message: distance: " + 
										Double.toString(gdm.distance) + " / score: " +
										Double.toString(gdm.score));
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
