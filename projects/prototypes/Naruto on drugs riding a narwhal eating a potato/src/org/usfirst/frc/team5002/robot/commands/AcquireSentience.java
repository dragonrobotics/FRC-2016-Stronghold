package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * This command picks up the ball using the limit switch.
 */
public class AcquireSentience extends Command {

    public AcquireSentience() {
        requires(Robot.belt);
    	this.setTimeout(10.0);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    	
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	Robot.belt.set(-0.4);
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return Robot.belt.getBallSwitch() || this.isTimedOut();
    }

    // Called once after isFinished returns true
    protected void end() {
    	Robot.belt.set(0);
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	end();
    }
}
