package netpacket;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

public class GoalDistanceMessage extends NetworkMessage {
	public enum Status {
		GOAL_FOUND,			// 255
		GOAL_NOT_FOUND;		// 0
	};
	
	public Status goal_status;
	public double distance;
	public double score;
	
	public GoalDistanceMessage() {
		goal_status = Status.GOAL_NOT_FOUND;
		distance = 0;
		score = 0;
		
		msgType = 4;
		responseType = null;
	}
	
	@Override
	public void writeObjectTo(ObjectOutputStream out) throws IOException {
		if(goal_status == Status.GOAL_FOUND) {
			out.writeByte((byte)255);
		} else {
			out.writeByte(0);
		}
		
		this.putDouble(out, distance);
		this.putDouble(out, score);
	}
	
	@Override
	public void readObjectFrom(ObjectInputStream in) throws IOException {
		int status_byte = in.readUnsignedByte();
		if(status_byte == 255) {
			goal_status = Status.GOAL_FOUND;
		} else {
			goal_status = Status.GOAL_NOT_FOUND;
		}
		
		distance = this.getDouble(in);
		score = this.getDouble(in);
	}
}
