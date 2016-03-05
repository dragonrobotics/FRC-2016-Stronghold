package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * Gets the POV for the joystick
 */
public class POV extends Command {
	private Command RaiseTheRoof;
	private Command SadPug;

	public POV() {
		RaiseTheRoof = new TOUCHDOWN();
		SadPug = new WhatAreThose();
	}

	protected void initialize() {
		if (Robot.oi.getJoystick().getPOV() == 0) {
			RaiseTheRoof.start();
		}
		if (Robot.oi.getJoystick().getPOV() == 180) {
			SadPug.start();
		}
	}

	protected void execute() {
	}

	protected boolean isFinished() {
		return false;
	}

	protected void end() {
	}

	protected void interrupted() {
	}
}
