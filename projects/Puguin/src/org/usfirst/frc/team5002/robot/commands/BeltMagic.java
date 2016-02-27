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

	protected void execute() {
		Robot.belt.run(-1.0);
	}

	protected boolean isFinished() {
		return Robot.launcher.getballswitch();
	}

	protected void end() {
		Robot.belt.stop();
	}

	protected void interrupted() {
		end();
	}
}
