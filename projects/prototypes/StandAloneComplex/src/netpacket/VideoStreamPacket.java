package netpacket;

import java.io.IOException;
import java.nio.ByteBuffer;
import edu.wpi.first.wpilibj.vision.USBCamera;

public class VideoStreamPacket extends NetworkMessage {

	ByteBuffer imageData;
	int bufsz;
	
	public void setBuffer(ByteBuffer buf, int size) {
			imageData = buf;
			bufsz = size;
	}
	
	public VideoStreamPacket(USBCamera grabCam) {
		
	}
	
	@Override
	public void writeObjectTo(ByteBuffer out) throws IOException {
		
	}

	@Override
	public void readObjectFrom(ByteBuffer in) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("WPILib's Vision class can't be converted from raw data, yell at them instead.");
	}

	@Override
	public byte getMessageID() {
		// TODO Auto-generated method stub
		return 6;
	}

	@Override
	public short getMessageSize() {
		// TODO Auto-generated method stub
		return 0;
	}

}
