package org.usfirst.frc.team5002.robot.subsystems.network;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

public class GoalDistanceMessage extends NetworkMessage implements Serializable {
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
		
		msgType = 1;
		responseType = null;
	}
	
	public void writeObjectTo(ObjectOutputStream out) throws IOException {
		if(goal_status == Status.GOAL_FOUND) {
			out.writeByte((byte)255);
		} else {
			out.writeByte(0);
		}
		
		String dstr = Double.toString(distance);
		String sstr = Double.toString(score);
		
		out.writeShort((short)dstr.length()+1);
		for(int i=0;i<dstr.length();i++) {
			out.writeByte((byte)dstr.charAt(i));
		}
		out.writeByte(0);
		
		out.writeShort((short)sstr.length()+1);
		for(int i=0;i<sstr.length();i++) {
			out.writeByte((byte)sstr.charAt(i));
		}
		out.writeByte(0);
	}
	
	public static GoalDistanceMessage readObjectFrom(ObjectInputStream in) throws IOException {
		GoalDistanceMessage out = new GoalDistanceMessage();
		
		int status_byte = in.readUnsignedByte();
		if(status_byte == 255) {
			out.goal_status = Status.GOAL_FOUND;
		} else {
			out.goal_status = Status.GOAL_NOT_FOUND;
		}
		
		short dlen = in.readShort();
		StringBuilder dstr = new StringBuilder();
		for(int i=0;i<dlen;i++) {
			int c = in.readUnsignedByte();
			if(c == 0)
				break;
			dstr.append(Character.toString((char) c));
		}
		
		short slen = in.readShort();
		StringBuilder sstr = new StringBuilder();
		for(int i=0;i<slen;i++) {
			int c = in.readUnsignedByte();
			if(c == 0)
				break;
			sstr.append(Character.toString((char) c));
		}
		
		out.distance = Double.parseDouble(dstr.toString());
		out.score = Double.parseDouble(sstr.toString());
		
		return out;
	}
	
	private void readObjectNoData() throws ObjectStreamException {
		throw new InvalidObjectException("Cannot unserialize empty bytestream");
	}
}
