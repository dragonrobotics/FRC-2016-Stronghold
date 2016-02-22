package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.CANTalon.FeedbackDevice;

/**
 * this project stops us from accidentally killing people by running them over,
 * shooting rogue balls, and basically shutting down the machinery
 */
public class Uselessthings extends Command {
	public Uselessthings() {
	}

	protected void initialize() {
	}

	protected void execute() {
		/*
		 * Chase Stockton: TODO: make "emergency" method in the subsystems.
		 * Stopping them won't do much of anything. Also, make another method in
		 * Robot.java that cancels all active commands and restarts this safety
		 * command along with any other command that should be running in the
		 * background (i.e. TriggerHappy).
		 */
		if (!Robot.drivetrain.isSafe()) {
			Robot.drivetrain.stop();
		}
		if (!Robot.launcher.isSafe()) {
			Robot.launcher.stop();
		}
		if (!Robot.belt.isSafe()) {
			Robot.belt.stop();
		}
		if (!Robot.thosearmthings.isSafe()) {
			Robot.thosearmthings.stop();
		}
	}

	protected boolean isFinished() {
		return false;
	}

	protected void end() {
	}

	protected void interrupted() {
	}
}
