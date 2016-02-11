package netpacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DiscoverPacket extends NetworkMessage {
	/*
	 enum class origin_t : unsigned char {
		DRIVER_STATION = 0,
		ROBORIO = 1,
		JETSON = 2,
		UNKNOWN = 0xFF
	 */
	
	enum origin_type {
		DRIVER_STATION,
		ROBORIO,
		JETSON,
		UNKNOWN
	};
	
	origin_type originator;
	
	DiscoverPacket(origin_type t) {
		originator = t;
	}
	
	DiscoverPacket() {
		originator = origin_type.ROBORIO;
	}
	
	@Override
	public void writeObjectTo(ObjectOutputStream out) throws IOException {
		switch(originator) {
		case DRIVER_STATION:
			out.writeByte(0);
			break;
		case ROBORIO:
			out.writeByte(1);
			break;
		case JETSON:
			out.writeByte(2);
			break;
		default:
			out.writeByte(0xFF);
			break;
		}
	}

	@Override
	public void readObjectFrom(ObjectInputStream in) throws IOException {
		int typebyte = in.readUnsignedByte();
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

}
