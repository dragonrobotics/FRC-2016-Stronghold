package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * Makes the tape measurer reel out
 * (like when a pug sticks its toungue out to lick your face :3)
 */
public class PugKisses extends Command {
	public PugKisses() {
		requires(Robot.tongueofyellow);
	}

	protected void initialize() {
	}

	protected void execute() {
		Robot.tongueofyellow.run(1.0);
	}

	protected boolean isFinished() {
		return false;
	}

	protected void end() {
		Robot.tongueofyellow.stop();
	}

	protected void interrupted() {
		end();
	}
}
