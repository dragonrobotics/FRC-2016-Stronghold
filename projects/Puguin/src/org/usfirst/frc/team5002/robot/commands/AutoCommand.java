package org.usfirst.frc.team5002.robot.commands;

import edu.wpi.first.wpilibj.command.Command;

/**
 * Command for main autonomous mode logic.
 * 
 * TODO: Is currently a no-op.
 */
public class AutoCommand extends Command {
	public AutoCommand() {

	}

	protected void initialize() {
	}

	protected void execute() {
	}

	protected boolean isFinished() {
		return false;
	}

	protected void end() {
	}

	protected void interrupted() {
		end();
	}
}