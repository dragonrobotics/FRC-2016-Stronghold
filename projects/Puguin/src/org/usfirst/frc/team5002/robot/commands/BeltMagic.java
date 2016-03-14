package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * Makes the belt run until the ball is in position to be launched.
 */
public class BeltMagic extends Command {
	public BeltMagic() {
		requires(Robot.belt);
	}

	protected void initialize() {

	}

	/**
	 * runs the belt backwards at max speed
	 */
	protected void execute() {
		Robot.belt.runBackwards();
	}

	protected boolean isFinished() {
		return false;
	}

	/**
	 * stops the belt
	 */
	protected void end() {
		Robot.belt.stop();
	}

	protected void interrupted() {
		end();
	}
}
