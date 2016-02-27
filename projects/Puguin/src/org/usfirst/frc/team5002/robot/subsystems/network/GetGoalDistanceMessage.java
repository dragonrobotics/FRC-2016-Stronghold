package org.usfirst.frc.team5002.robot.subsystems.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Network message requesting goal data from the Jetson.
 */
public class GetGoalDistanceMessage extends NetworkMessage {
	public GetGoalDistanceMessage(InetAddress address) {
		this.addr = address;
	}

	/* Stub methods. There's nothing here to transmit! */
	@Override
	public void writeObjectTo(ByteBuffer out) throws IOException {
	}

	@Override
	public void readObjectFrom(ByteBuffer in) throws IOException {
	}

	@Override
	public byte getMessageID() {
		return 3;
	}

	@Override
	public short getMessageSize() {
		return 0;
	}
}
