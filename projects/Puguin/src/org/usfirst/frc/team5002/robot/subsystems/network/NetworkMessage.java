package org.usfirst.frc.team5002.robot.subsystems.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class NetworkMessage {
	public Class responseType = null;
	public char msgType = 0;
	
	abstract public void writeObjectTo(ObjectOutputStream out) throws IOException;
	abstract public void readObjectFrom(ObjectInputStream in) throws IOException;
}
