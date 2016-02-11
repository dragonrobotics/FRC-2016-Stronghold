package netpacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;

public class GetGoalDistanceMessage extends NetworkMessage {
	public char msgType = 3;
	
	public GetGoalDistanceMessage(InetAddress address) {
		this.addr = address;
	}
	
	/* Stub methods. There's nothing here to transmit! */
	@Override
	public void writeObjectTo(ObjectOutputStream out) throws IOException {}
	
	@Override
	public void readObjectFrom(ObjectInputStream in) throws IOException {}
}
