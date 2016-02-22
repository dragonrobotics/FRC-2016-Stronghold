package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 *
 */
public class Automove extends Command {
	private double x, y, angle;

	public Automove(double x, double y, double angle) {
		requires(Robot.drivetrain);
		this.x = x;
		this.y = y;
		this.angle = angle;
	}

	protected void initialize() {
	}

	protected void execute() {
		Robot.drivetrain.autoDrive(x, y, angle);
	}

	protected boolean isFinished() {
		return Robot.drivetrain.isInPosition();
	}

	protected void end() {
		Robot.drivetrain.stop();
	}

	protected void interrupted() {
		end();
	}
}