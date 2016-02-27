package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * makes the arm go down on the robot 
 * (The title "WhaAreThose" is a reference.
 * If you don't understand the reference
 * you need to google it.)
 */
public class WhatAreThose extends Command {
	public WhatAreThose() {
		requires(Robot.thosearmthings);
	}

	protected void initialize() {

	}

	protected void execute() {
		Robot.thosearmthings.armsdown();
	}

	protected boolean isFinished() {
		return false;
	}

	protected void end() {
		Robot.thosearmthings.stop();
	}

	protected void interrupted() {
		end();
	}
}
