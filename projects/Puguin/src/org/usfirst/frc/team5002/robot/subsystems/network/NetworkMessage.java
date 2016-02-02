package org.usfirst.frc.team5002.robot.subsystems.network;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class NetworkMessage implements Serializable {
	public Class responseType = null;
	public char msgType = 0;
}
