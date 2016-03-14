package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * Drives the robot to the decisive battle, poi~!
 */
public class Poi extends Command {
	double targetX;
	double targetY;
	double finalTurnAngle;
	
	boolean taiha = false;
	
	/***
	 * Poi(boolean, double) - Start driving, poi~!
	 * @param tDist - how far we want to be from the goal in the end, poi~
	 */
	public Poi(double tDist) {
		requires(Robot.drivetrain);
		
		if(Robot.jetson.isDaijoubu()) {
			double startDistance = Robot.jetson.getDistance();
			double startAngle = Robot.jetson.getAngle() + Robot.getRobotYaw();
			
			// transform to robot-local cartesian coordinates.
			// remember that 0 degrees heading is directly forward from starting orientation (Y axis), so we swap sin/cos.
			targetY = startDistance * Math.cos(Math.toRadians(startAngle));
			targetX = startDistance * Math.sin(Math.toRadians(startAngle));
			
			targetY -= tDist;
			
			// move vector = (atan2(tY, tX), hypot(tX, tY))
			// final turn = -(move vector angle)
			
			finalTurnAngle = Math.floor(Robot.getRobotYaw() / 360.0) * 360.0;
		} else {
			taiha = true;
		}
	}

	protected void initialize() {}

	protected void execute() {
		Robot.drivetrain.autoDrive(targetX, targetY);
		Robot.drivetrain.autoTurn(finalTurnAngle);
	}

	protected boolean isFinished() {
//		return taiha || Robot.drivetrain.isInPosition();
		return true;
	}

	protected void end() {}

	protected void interrupted() {}
}
