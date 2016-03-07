package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * Drives the robot to the decisive battle, poi~!
 */
public class Poi extends Command {
	double targetAngle;
	double targetDistance;
	
	boolean onTargetAngle = false;
	
	boolean taiha = false;
	
	State state;
	
	/***
	 * Poi(boolean, double) - Start driving, poi~!
	 * @param tDist - how far we want to be from the goal in the end, poi~
	 */
	public Poi(double tDist) {
		requires(Robot.drivetrain);
		
		if(Robot.jetson.isDaijoubu()) {
			rightOfGoal = right;
			
			double startDistance = Robot.jetson.getDistance();
			double startAngle = Robot.jetson.getAngle() + (Robot.getRobotAngle() % 360.0);
			
			// transform to robot-local cartesian coordinates.
			// remember that 0 degrees heading is directly forward from starting orientation (Y axis), so we swap sin/cos.
			double targetY = startDistance * Math.cos(Math.toRadians(startAngle));
			double targetX = startDistance * Math.sin(Math.toRadians(startAngle));
			
			targetY -= tDist;
			
			targetAngle = Math.atan2(targetY, targetX);
			targetDistance = Math.sqrt(Math.pow(targetX, 2) + Math.pow(targetY, 2));
		} else {
			taiha = true;
		}
	}

	protected void initialize() {
		
	}

	protected void execute() {
		double curAngleErr = endAngle - Robot.getRobotAngle();
		
		if(Math.abs(curAngleErr) > 1) {
			onTargetAngle = false;
			
			mcLT.changeControlMode(TalonControlMode.Speed);
			mcRT.changeControlMode(TalonControlMode.Speed);
		
			if(Math.abs(curAngleErr) > 100)
				curAngleErr = 100 * (curAngleErr < 0 ? -1 : 1);
			
			double out = (Math.abs(curAngleErr) / 100) * maxTurnOutput;
			
			if(curAngleErr > 0) {
				mcLT.set(out);
				mcRT.set(-out);
			} else {
				mcLT.set(-out);
				mcRT.set(out);
			}
			
			mcLT.changeControlMode(TalonControlMode.Position);
			mcRT.changeControlMode(TalonControlMode.Position);
		} else {
			onTargetAngle = true;
			
			mcLT.changeControlMode(TalonControlMode.Position);
			mcRT.changeControlMode(TalonControlMode.Position);
			
			mcLT.set(targetDistance);
			mcRT.set(targetDistance);
		}
	}

	protected boolean isFinished() {
		return taiha || (onTargetAngle && Robot.drivetrain.isInPosition());
	}

	protected void end() {
		mcLT.changeControlMode(TalonControlMode.Position);
		mcRT.changeControlMode(TalonControlMode.Position);
	}

	protected void interrupted() {
		mcLT.changeControlMode(TalonControlMode.Position);
		mcRT.changeControlMode(TalonControlMode.Position);
	}
}
