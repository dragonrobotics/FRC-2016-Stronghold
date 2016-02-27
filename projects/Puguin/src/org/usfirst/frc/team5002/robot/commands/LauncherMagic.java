package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * this is the code for the button on the controller that will use the
 * shooting mechanism
 */
public class LauncherMagic extends Command {
	public LauncherMagic() {
		requires(Robot.launcher);
	}

	protected void initialize() {
	}

	protected void execute() {
		if (Robot.launcher.getballswitch()) {
			Robot.launcher.run();
		} else {
			Robot.launcher.stop();
		}
	}

	protected boolean isFinished() {
		return false;
	}

	protected void end() {
		Robot.launcher.stop();
	}

	protected void interrupted() {
		end();
	}
}
