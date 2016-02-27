package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * This codes for the Joystick when Teleop Drive is activated
 * (The name of the code is a play off of Doctor Who's quote, 
 * "Wibbly wobbly timey wimey," because we're all nerds here.)
 */
public class TeleopDriveyWivey extends Command {
	public TeleopDriveyWivey() {
		requires(Robot.drivetrain);
	}

	protected void initialize() {
	}

	protected void execute() {
		Robot.drivetrain.joystickDrive(Robot.oi.getJoystick());
	}

	protected boolean isFinished() {
		return false;
	}

	protected void end() {
		Robot.drivetrain.stop();
	}

	protected void interrupted() {
		end();
	}
}
