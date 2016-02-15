package org.usfirst.frc.team5002.robot.subsystems.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class DiscoverPacket extends NetworkMessage {
	

	
	/*
	 enum class origin_t : unsigned char {
		DRIVER_STATION = 0,
		ROBORIO = 1,
		JETSON = 2,
		UNKNOWN = 0xFF
	 */
	
	public enum origin_type {
		DRIVER_STATION,
		ROBORIO,
		JETSON,
		UNKNOWN
	};
	
	public origin_type originator;
	
	public DiscoverPacket(InetAddress addr, origin_type t) {
		originator = t;
		this.addr = addr;
	}
	
	public DiscoverPacket(InetAddress addr) {
		originator = origin_type.ROBORIO;
		this.addr = addr;
	}
	
	@Override
	public void writeObjectTo(ByteBuffer out) throws IOException {
		switch(originator) {
		case DRIVER_STATION:
			out.put((byte) 0);
			break;
		case ROBORIO:
			out.put((byte) 1);
			break;
		case JETSON:
			out.put((byte) 2);
			break;
		default:
			out.put((byte) 0xFF);
			break;
		}
	}

	@Override
	public void readObjectFrom(ByteBuffer in) throws IOException {
		byte typebyte = in.get();
		switch(typebyte) {
		case 0:
			originator = origin_type.DRIVER_STATION;
			break;
		case 1:
			originator = origin_type.ROBORIO;
			break;
		case 2:
			originator = origin_type.JETSON;
			break;
		default:
			originator = origin_type.UNKNOWN;
		}
	}

	@Override
	public byte getMessageID() {
		return 5;
	}
	
	@Override
	public short getMessageSize() {
		return 1;
	}
}
