package org.usfirst.frc.team5002.robot.commands;

import java.io.IOException;
import java.net.Socket;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import netpacket.DiscoverPacket;
import netpacket.GetGoalDistanceMessage;
import netpacket.GoalDistanceMessage;
import netpacket.GoalDistanceMessage.Status;
import netpacket.NetworkMessage;

/**
 *
 */
public class JetsonListen extends Command {

    public JetsonListen() {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
    	requires(Robot.jetson);
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    @SuppressWarnings("deprecation")
	protected void execute() {
    	NetworkMessage msg;
    	SmartDashboard.putString("Current Status", "Waiting on Jetson...");
    	try {
			Robot.jetson.sendMessage(new GetGoalDistanceMessage(Robot.jetson.getJetsonAddress()));
			msg = Robot.jetson.readMessage();
    	} catch (IOException e) {
			e.printStackTrace();
			return;
		}
    	
		if(msg instanceof GoalDistanceMessage) {
			GoalDistanceMessage dmsg = (GoalDistanceMessage)msg;
			if(dmsg.goal_status == Status.GOAL_FOUND) {
				SmartDashboard.putString("Current Status", "Goal found.");
				SmartDashboard.putNumber("Current Score", dmsg.score);
				SmartDashboard.putNumber("Current Distance", dmsg.distance);
			} else {
				SmartDashboard.putString("Current Status", "Goal not found!");
				SmartDashboard.putNumber("Current Score", dmsg.score);
				SmartDashboard.putNumber("Current Distance", dmsg.distance);
			}
		} else {
			SmartDashboard.putString("Current Status", "Waiting on Jetson...");
		}
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
