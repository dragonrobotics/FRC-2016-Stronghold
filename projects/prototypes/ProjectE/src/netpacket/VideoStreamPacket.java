package netpacket;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ni.vision.NIVision;

import edu.wpi.first.wpilibj.vision.USBCamera;

// Warning: This class is highly dependent on the format of the
// data returned from the webcam. I have no way of retrieving this data automatically,
// So I'm just plugging in values that /should/ work and hoping that they're right.
public class VideoStreamPacket extends NetworkMessage {

	public final int imgWidth = 800;
	public final int imgHeight = 640;
	
	ByteBuffer imageData;
	
	public void setBuffer(ByteBuffer buf) {
			imageData = buf;
	}
	
	public void captureFromCamera(USBCamera camera) {
		imageData = ByteBuffer.allocate(imgWidth * imgHeight * 3);
		camera.setSize(imgWidth, imgHeight);
		camera.getImageData(imageData);
		
	}
	
	@Override
	public void writeObjectTo(ByteBuffer out) throws IOException {
		out.putShort((short) imgWidth);
		out.putShort((short) imgHeight);
		out.putInt(0); // 23 = CV_8UC3
		out.put((byte)1);
		out.put(imageData);
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
		return (short) (7 + imageData.capacity());
	}

}
