package org.usfirst.frc.team5002.robot.commands;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.command.Command;

/**
 * Changes which camera is in use in order to alter
 * the driver's perspective
 */
public class BipolarCamera extends Command {
	CameraServer server;
	private boolean camera;
	
	
    public BipolarCamera() {
    	server = CameraServer.getInstance();
        server.setQuality(50);
        //the camera name (ex "cam0") can be found through the roborio web interface
        server.startAutomaticCapture("cam0");
        camera = true;
    }
    // Called just before this Command runs the first time
    protected void initialize() {
    	if (camera){
    		server.startAutomaticCapture("cam1");
    	}
    	else{
    		server.startAutomaticCapture("cam0");
    	}
    	camera = !camera;
    }

    // Called repeatedly when this Command is scheduled to run
    protected void execute() {
    }

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return true; //has to return true
    }

    // Called once after isFinished returns true
    protected void end() {
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    }
}
