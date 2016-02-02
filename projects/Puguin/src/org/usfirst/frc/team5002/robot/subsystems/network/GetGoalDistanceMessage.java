package org.usfirst.frc.team5002.robot.subsystems.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@SuppressWarnings("serial")
public class GetGoalDistanceMessage extends NetworkMessage {
	public Class responseType = GoalDistanceMessage.class;
	public char msgType = 3;
	
	/* Stub methods. There's nothing here to transmit! */
	@Override
	public void writeObjectTo(ObjectOutputStream out) throws IOException {}
	
	@Override
	public void readObjectFrom(ObjectInputStream in) throws IOException {}
}
