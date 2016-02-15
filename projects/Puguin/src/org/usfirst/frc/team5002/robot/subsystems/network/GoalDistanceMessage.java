package org.usfirst.frc.team5002.robot.subsystems.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class GoalDistanceMessage extends NetworkMessage {
	public enum Status {
		GOAL_FOUND,			// 255
		GOAL_NOT_FOUND;		// 0
	};
	
	public Status goal_status;
	public double distance;
	public double score;
	
	public GoalDistanceMessage(InetAddress addr) {
		goal_status = Status.GOAL_NOT_FOUND;
		distance = 0;
		score = 0;
		this.addr = addr;
	}
	
	@Override
	public void writeObjectTo(ByteBuffer out) throws IOException {
		if(goal_status == Status.GOAL_FOUND) {
			out.put((byte)255);
		} else {
			out.put((byte) 0);
		}
		
		this.putDouble(out, distance);
		this.putDouble(out, score);
	}
	
	@Override
	public void readObjectFrom(ByteBuffer in) throws IOException {
		byte status_byte = in.get();
		if(status_byte == 255) {
			goal_status = Status.GOAL_FOUND;
		} else {
			goal_status = Status.GOAL_NOT_FOUND;
		}
		
		distance = this.getDouble(in);
		score = this.getDouble(in);
	}
	
	@Override
	public byte getMessageID() {
		return 4;
	}
	
	@Override
	public short getMessageSize() {
		return (short)(Double.toString(distance).length()+Double.toString(score).length()+4+1);
	}
}
