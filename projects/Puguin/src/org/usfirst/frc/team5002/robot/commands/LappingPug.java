package org.usfirst.frc.team5002.robot.commands;

import edu.wpi.first.wpilibj.command.Command;

/**
 * makes the tape measurer come back in (like when a pug is drinking water and they draw their tounge back in)
 */
public class LappingPug extends Command {

    public LappingPug() {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    }
}
