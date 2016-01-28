// "Sir, have you or your robot been drinking, tonight?"
// "I'm afraid the robot's been drinking, officer. Good thing he's not in the driver's se-- oh, wait."
package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 *
 */
public class GetOdometryData extends Command {
	private static boolean initialized = false;
	
	private static double lastLDist = 0;
	private static double lastRDist = 0;
	
	private static double wheelBase = 0; /* TODO: Fill this in with the distance between the wheels */
	
    public GetOdometryData() {
        // This *shouldn't* depend on anything
    	// reading the encoders shouldn't mess with the motors
    	if(!initialized) {
    		lastLDist = Robot.drivetrain.getLeftControlData();
    		lastRDist = Robot.drivetrain.getRightControlData();
    		initialized = true;
    	}
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    	if(initialized) {
    		double curLEnc = Robot.drivetrain.getLeftControlData();
    		double curREnc = Robot.drivetrain.getRightControlData();
    		
	    	double deltaL = curLEnc - lastLDist;
	    	double deltaR = curREnc - lastRDist;
	    	
	    	double distPrime = (deltaL - deltaR) / 2;
	    	double thetaPrime = (deltaL - deltaR) / wheelBase;
	    	
	    	Robot.loc.addPositionUpdate(distPrime * Math.cos(thetaPrime), distPrime * Math.sin(thetaPrime));
	    	Robot.loc.addHeadingUpdate(thetaPrime);
	    	
	    	lastLDist = curLEnc;
	    	lastRDist = curREnc;
    	}
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return true;
    }

    // Called once after isFinished returns true
    protected void end() {
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    }
}
