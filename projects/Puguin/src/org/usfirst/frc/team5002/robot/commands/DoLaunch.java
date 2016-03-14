package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * Command to do max-power launches.
 * 
 * Requires Belt and Launcher subsystems.
 */
public class DoLaunch extends Command {
	/**
	 * this works the shooting mechanism
	 */
	public DoLaunch() {
		requires(Robot.launcher);
		requires(Robot.belt);
		this.setTimeout(5);
	}

	protected void initialize() {
	}

	protected void execute() {
		if (this.timeSinceInitialized() > 1) {
			Robot.belt.run();
		}
		Robot.launcher.run(.75);
	}

	protected boolean isFinished() {
		return this.isTimedOut();
	}

	protected void end() {
		Robot.belt.stop();
		Robot.launcher.stop();
	}

	protected void interrupted() {
		end();
	}
}
