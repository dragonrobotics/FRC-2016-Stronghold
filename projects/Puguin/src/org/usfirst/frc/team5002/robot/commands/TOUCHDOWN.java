package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * makes arms on robot lift in an upwards motion
 *  (like when the referee stands up and yells "TOUCHDOWN" 
 *  really loudly while lifting his arms)
 */
public class TOUCHDOWN extends Command {
	public TOUCHDOWN() {
		requires(Robot.thosearmthings);
	}

	protected void initialize() {
	}

	protected void execute() {
		Robot.thosearmthings.armsup();
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
