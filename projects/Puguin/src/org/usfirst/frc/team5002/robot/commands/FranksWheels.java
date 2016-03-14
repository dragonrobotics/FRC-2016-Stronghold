 package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * controls the flywheels on the front of the robot (it is Frank from MIB) this
 * works the wheels
 * 
 * TODO: Is currently a no-op.
 */
public class FranksWheels extends Command {
	public FranksWheels() {
		requires(Robot.barofwheels);
	}

	protected void initialize() {
	}

	/**
	 * runs motor at max speed
	 */
	protected void execute() {
		Robot.barofwheels.runbackwards();
	}

	protected boolean isFinished() {
		return false;
	}

	/**
	 * stops the motor
	 */
	protected void end() {
		Robot.barofwheels.stop();
	}

	protected void interrupted() {
		end();
	}
}
