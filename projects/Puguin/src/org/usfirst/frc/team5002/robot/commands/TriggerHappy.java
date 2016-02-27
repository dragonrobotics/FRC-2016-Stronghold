package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * Gets the axes for the triggers on the joystick
 * to ensure a successful launch
 * (and so build team/Chase don't yell at us)
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
