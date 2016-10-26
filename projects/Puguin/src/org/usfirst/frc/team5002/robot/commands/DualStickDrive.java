package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;
import org.usfirst.frc.team5002.robot.subsystems.Drivetrain;
import edu.wpi.first.wpilibj.command.Command;

/**
 * This codes for the Joystick when Teleop Drive is activated
 * (The name of the code is a play off of Doctor Who's quote,
 * "Wibbly wobbly timey wimey," because we're all nerds here.)
 */
public class DualStickDrive extends Command {
	int l;
	int r;
	double deadband;

	public DualStickDrive(int lAxis, int rAxis, double db) {
		requires(Robot.drivetrain);
		this.l = lAxis;
		this.r = rAxis;
		this.deadband = db;
	}

	protected void initialize() {
	}

	protected void execute() {
		Robot.drivetrain.dualStickDrive(this.l, this.r, this.deadband);
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
