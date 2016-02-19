package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * makes arms on robot go up (like a when that fat guy at football games 
 * stands up and yells "TOUCHDOWN" really loudly while lifting his arms)
 */
public class TOUCHDOWN extends Command {

    public TOUCHDOWN() {
        requires(Robot.thosearmthings); 
        // eg. requires(ch  assis);
    }

    // Called just before this Command runs the first time
    protected void initialize() { 
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	Robot.thosearmthings.armsup();
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    	Robot.thosearmthings.stop();
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	end();
    }
}
