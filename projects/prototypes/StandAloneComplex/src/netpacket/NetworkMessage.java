package netpacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;

public abstract class NetworkMessage {
	public char msgType;
	public InetAddress addr;
	
	public String getLenString(ObjectInputStream in) throws IOException {
		short len = in.readShort();
		StringBuilder str = new StringBuilder();
		for(int i=0;i<len;i++) {
			int c = in.readUnsignedByte();
			if(c == 0)
				break;
			str.append(Character.toString((char) c));
		}
		return str.toString();
	}
	
	public String getNullTermString(ObjectInputStream in) throws IOException {
		StringBuilder str = new StringBuilder();
		while(true) {
			int c = in.readUnsignedByte();
			if(c == 0)
				break;
			str.append(Character.toString((char) c));
		}
		return str.toString();
	}
	
	public void putLenString(ObjectOutputStream out, String str) throws IOException {
		out.writeShort(str.length());
		for(int i=0;i<str.length();i++) {
			out.writeByte((byte)str.charAt(i));
		}
	}
	
	public void putNullTermString(ObjectOutputStream out, String str) throws IOException {
		for(int i=0;i<str.length();i++) {
			out.writeByte((byte)str.charAt(i));
		}
		out.writeByte(0);
	}
	
	public double getDouble(ObjectInputStream in) throws IOException {
		return Double.parseDouble(this.getLenString(in));
	}
	
	public void putDouble(ObjectOutputStream out, double d) throws IOException {
		this.putLenString(out, Double.toString(d));
	}
	
	abstract public void writeObjectTo(ObjectOutputStream out) throws IOException;
	abstract public void readObjectFrom(ObjectInputStream in) throws IOException;
}
