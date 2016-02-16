package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.command.Command;

/**
 * <p>
 * This command uses the joystick to set the speed of the pitcher.
 * </p>
 * <p>
 * This is the default command of the pitcher subsystem.
 * </p>
 */
public class PugstickAttack extends Command {
	// you should be able to mouse over methods for explanations

	/**
	 * <p>
	 * The constructor. Called when a new instance of this class is made.
	 * </p>
	 * <p>
	 * Here is where you should require all subsystems you use in this command.
	 * </p>
	 */
	public PugstickAttack() {
		requires(Robot.pitcher);
	}

	protected void initialize() {

	}

	protected void execute() {
		// see RestrainKoala.java for if statement shortcut below
		Joystick stick = Robot.oi.getJoystick();
		// only gives spin if button 8 is pressed
//		Robot.pitcher.set(-stick.getY(), stick.getRawButton(8) ? stick.getThrottle() : 0);
		Robot.pitcher.set(-stick.getY(), stick.getThrottle());
	}

	protected boolean isFinished() {
		return false;
	}

	protected void end() {
		Robot.pitcher.set(0, 0);
	}

	protected void interrupted() {
		end();
	}
}
