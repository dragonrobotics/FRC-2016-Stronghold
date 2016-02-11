package nettest;

import java.io.IOException;

public class NetTestMain {

	public static void main(String[] args) {
		try {
			Jetson jetson = new Jetson();
			jetson.doDiscover();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
