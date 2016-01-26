package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.command.Command;

/**
 * This command freezes the pitcher at its current speed.
 */
public class FireKoala extends Command {
	// you should be able to mouse over methods for explanations

	double speed, spin;

	/**
	 * <p>
	 * The constructor. Called when a new instance of this class is made.
	 * </p>
	 * <p>
	 * Here is where you should require all subsystems you use in this command.
	 * </p>
	 */
	public FireKoala() {
		requires(Robot.pitcher);
	}

	protected void initialize() {
		/*
		 * if statement shortcut:
		 * 
		 * spin = stick.getRawButton(2) ? stick.getX() : 0;
		 * 
		 * ...is equivalent to:
		 * 
		 * if (stick.getRawButton(2)) {
		 *     spin = stick.getX();
		 * } else {
		 *     spin = 0;
		 * }
		 * 
		 */
		Joystick stick = Robot.oi.getJoystick();
		speed = stick.getY();
		// only assigns nonzero spin if button 2 is pressed
		spin = stick.getRawButton(2) ? stick.getX() : 0;
	}

	protected void execute() {
		Robot.pitcher.set(speed, spin);
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
