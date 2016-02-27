package org.usfirst.frc.team5002.robot.subsystems.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Base class for all of the messages sent over and received from the network.
 * Also implements functions for sending and receiving complex datatypes sanely over the network,
 * because Java is stupid and does weird shit to its strings (and presumably doubles)
 * 
 */
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

	/**
	 * Writes the message to a ByteBuffer for transmission.
	 * @param out Output byte buffer
	 * @throws IOException
	 */
	abstract public void writeObjectTo(ByteBuffer out) throws IOException;

	/**
	 * Reads the message from a byte buffer (from a received message)
	 * @param in Byte buffer containing the message as raw bytes
	 * @throws IOException
	 */
	abstract public void readObjectFrom(ByteBuffer in) throws IOException;

	/**
	 * Gets the network message's 1-byte type ID.
	 * @return the type identifier for this network message.
	 */
	abstract public byte getMessageID();

	/**
	 * Gets the size of the network message when serialized.
	 * @return size of the serialized network message.
	 */
	abstract public short getMessageSize();
}
