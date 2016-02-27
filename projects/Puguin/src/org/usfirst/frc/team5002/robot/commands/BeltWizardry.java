package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;
import edu.wpi.first.wpilibj.command.Command;

/**
 * Runs the belt forever and ever and ever. until the code says to stop.
 */
public class BeltWizardry extends Command {
	public BeltWizardry() {
		requires(Robot.belt);
	}

	protected void initialize() {
	}

	/**
	 * runs the belt forward at max speed
	 */
	protected void execute() {
		Robot.belt.run(1.0);
	}

	protected boolean isFinished() {
		return false;
	}

	/**
	 * stops the belt.
	 */
	protected void end() {
		Robot.belt.stop();
	}

	protected void interrupted() {
		end();
	}
}
