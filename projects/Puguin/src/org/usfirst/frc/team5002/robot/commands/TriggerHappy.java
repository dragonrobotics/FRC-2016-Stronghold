package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * shoots the balls because that's important and stuff
 */
public class TriggerHappy extends Command {
	private Command FireBalls;
	private Command GetBalls;

	public TriggerHappy() {
		FireBalls = new DoLaunch();
		GetBalls = new BeltMagic();
	}

	protected void initialize() {
	}

	protected void execute() {
		if (Robot.oi.getJoystick().getRawAxis(3) == -1) {
			FireBalls.start();
		}
		if (Robot.oi.getJoystick().getRawAxis(3) == 1) {
			GetBalls.start();
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
