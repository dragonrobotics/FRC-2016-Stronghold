package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.command.Command;

/**
 * runs during designated autonomous period
 */
public class Autonomous extends Command {
	public Autonomous() {
	}

	protected void initialize() {

	}

	protected void execute() {
		Robot.drivetrain.autoDrive(1, 5);
	}

	protected boolean isFinished() {
		return false;
	}

	protected void end() {
	}

	protected void interrupted() {
		end();
	}
}
