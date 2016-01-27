package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 *
 */
public class SetCookieProduction extends Command {
	// you should be able to mouse over methods for explanations

	double speed;

	/**
	 * <p>
	 * The constructor. Called when a new instance of this class is made.
	 * </p>
	 * <p>
	 * Here is where you should require all subsystems you use in this command.
	 * </p>
	 * 
	 * @param speed - The speed you want this command to set the belts to.
	 */
	public SetCookieProduction(double speed) {
		this.speed = speed;
		requires(Robot.belt);
	}

	protected void initialize() {
	}

	protected void execute() {
		Robot.belt.set(speed);

	}

	protected boolean isFinished() {
		return false;
	}

	protected void end() {
		Robot.belt.set(0);
	}

	protected void interrupted() {
		end();
	}
}
