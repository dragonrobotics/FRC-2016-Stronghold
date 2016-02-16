package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 *
 */
public class FireKoala extends Command {

    public FireKoala() {
        requires(Robot.belt);
        requires(Robot.pitcher);
        setTimeout(5);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
	protected void execute() {
		Robot.pitcher.set(0.735, Robot.oi.getJoystick().getThrottle());
		if (timeSinceInitialized() > 2) {
			Robot.belt.set(-0.8);
    	}
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return this.isTimedOut();
    }

    // Called once after isFinished returns true
    protected void end() {
		Robot.belt.set(0);
		Robot.pitcher.set(0, 0);
	}

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	end();
    }
}
