package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;
import edu.wpi.first.wpilibj.command.Command;

/**
 * makes the tape measurer retract.
 * This shortens the length that is exposed.
 * (like when a pug is drinking water and they draw their tongue back in)
 * 
 * TODO: doesn't seem to have a stop condition
 */
public class LappingPug extends Command {

	public LappingPug() {
		requires(Robot.tongueofyellow);
	}

	protected void initialize() {
	}

	protected void execute() {
		Robot.tongueofyellow.run(-1.0);
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
