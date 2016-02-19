package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 *this (i think) is the code for the button on the controller that will use the shooting mechanism
 */
public class LauncherMagic extends Command {

    public LauncherMagic() {
    	requires(Robot.launcher);
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    //TODO: if switch is pressed then robot.launcher.run(); else Robot.launcher.stop();
    	if (Robot.launcher.getballswitch()){
    		Robot.launcher.run();
    	}
    	else {
    		Robot.launcher.stop();
    	}
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    	Robot.launcher.stop();
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    	end();
    }
}
