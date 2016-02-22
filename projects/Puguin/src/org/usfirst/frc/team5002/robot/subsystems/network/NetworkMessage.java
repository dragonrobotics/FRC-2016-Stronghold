package org.usfirst.frc.team5002.robot.subsystems.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public abstract class NetworkMessage {
	public InetAddress addr;

	public String getLenString(ByteBuffer in) throws IOException {
		short len = in.getShort();
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < len; i++) {
			byte c = in.get();
			if (c == 0)
				break;
			str.append(Character.toString((char) c));
		}
		return str.toString();
	}

	public String getNullTermString(ByteBuffer in) throws IOException {
		StringBuilder str = new StringBuilder();
		while (true) {
			byte c = in.get();
			if (c == 0)
				break;
			str.append(Character.toString((char) c));
		}
		return str.toString();
	}

	public void putLenString(ByteBuffer out, String str) throws IOException {
		out.putShort((short) str.length());
		for (int i = 0; i < str.length(); i++) {
			out.put((byte) str.charAt(i));
		}
	}

	public void putNullTermString(ByteBuffer out, String str) throws IOException {
		for (int i = 0; i < str.length(); i++) {
			out.put((byte) str.charAt(i));
		}
		out.put((byte) 0);
	}

	public double getDouble(ByteBuffer in) throws IOException {
		return Double.parseDouble(this.getLenString(in));
	}

	public void putDouble(ByteBuffer out, double d) throws IOException {
		this.putLenString(out, Double.toString(d));
	}

	abstract public void writeObjectTo(ByteBuffer out) throws IOException;

	abstract public void readObjectFrom(ByteBuffer in) throws IOException;

	abstract public byte getMessageID();

	abstract public short getMessageSize();
}
