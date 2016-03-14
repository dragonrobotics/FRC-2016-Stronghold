package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;
import edu.wpi.first.wpilibj.command.Command;

/**
 * Runs the belt forever and ever and ever. until the code says to stop.
 */
public class BeltWizardry extends Command {
	private double sec = 0;
	private boolean didHit;
	public BeltWizardry() {
		requires(Robot.belt);
	}

	protected void initialize() {
		didHit = false;
		
	}

	/**
	 * runs the belt forward at max speed
	 */
	protected void execute() {
		if (Robot.belt.getballswitch() && !didHit){
			didHit = true;
			sec = this.timeSinceInitialized();
		}
		if (!didHit || this.timeSinceInitialized() - sec < 0.75) {
			Robot.belt.run();	
		}
	}

	protected boolean isFinished() {
		return false;
		
	}

	/**
	 * stops the belt.
	 */
	protected void end() {
		Robot.belt.stop();
	}

	protected void interrupted() {
		end();
	}
}
