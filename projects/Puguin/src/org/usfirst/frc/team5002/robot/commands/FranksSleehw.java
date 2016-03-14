package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * runs flywheels backwards
 */
public class FranksSleehw extends Command {

	public FranksSleehw() {
		requires(Robot.barofwheels);
	}

	protected void initialize() {
	}

	/**
	 * run motor backwards at max speed
	 */
	protected void execute() {
		Robot.barofwheels.run();
	}

	protected boolean isFinished() {
		return false;
	}

	/**
	 * stops motor
	 */
	protected void end() {
		Robot.barofwheels.stop();
	}

	protected void interrupted() {
		end();
	}
}
