package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Command;

/**
 * this is the code for the button on the controller that will use the
 * shooting mechanism
 */
public class LauncherMagic extends Command {
	private double sec = 0;
	
	public LauncherMagic() {
		requires(Robot.launcher);
	
	}

	protected void initialize() {
		sec = Timer.getFPGATimestamp();
	}

	protected void execute() {
		if (Timer.getFPGATimestamp() - sec > 1){
			Robot.belt.run();
		}
		Robot.launcher.run(.75);
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
