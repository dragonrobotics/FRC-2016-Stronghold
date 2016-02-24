package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * Poi -- Drives the robot to the decisive battle, poi~!
 */
public class Poi extends Command {
	double angleToPerpendicular;
	double distanceToAlign;
	double distanceToTarget;
	
	boolean rightOfGoal = false;
	boolean taiha = false;
	boolean stateCommandSent = false;
	
	private enum State {
		TURN_PERPENDICULAR,
		ALIGN_X,
		TURN_TO_GOAL,
		ALIGN_Y,
		FINISHED,
	}
	
	State state;
	
	/***
	 * Poi(boolean, double) - Start driving, poi~!
	 * @param right - are we right of the target goal, poi~?
	 * @param tDist - how far we want to be from the goal in the end, poi~
	 */
	public Poi(boolean right, double tDist) {
		requires(Robot.drivetrain);
		
		if(Robot.jetson.isDaijoubu()) {
			rightOfGoal = right;
			
			double startDistance = Robot.jetson.getDistance();
			double startAngle = Robot.jetson.getAngle(); // heading diff
			
			// operating in the coordinate system of the goal:
			// targetDistance = tDist
			// targetAngle = 0
			
			// then transform to the coordinate system of the robot
			
			double ourX = startDistance * Math.cos(Math.toRadians(startAngle));
			double ourY = startDistance * Math.sin(Math.toRadians(startAngle));
			
			angleToPerpendicular = 90 - startAngle;
			
			// drive from (startAngle, startDistance) to (0, tDist):
			// turn so that we're perpendicular to goal
			// drive to align to goal
			// turn to face goal
			// drive to reach target distance
			
			distanceToAlign = startDistance * Math.cos(Math.toRadians(angleToPerpendicular));
			double goalDistAtAlign = startDistance * Math.sin(Math.toRadians(angleToPerpendicular));
			
			distanceToTarget = goalDistAtAlign - tDist;
			
			state = State.TURN_PERPENDICULAR;
			stateCommandSent = false;
		} else {
			taiha = true;
		}
	}

	protected void initialize() {
		
	}

	protected void execute() {
		switch(state) {
		case TURN_PERPENDICULAR:
			if(!stateCommandSent) {
				if(rightOfGoal) {
					Robot.drivetrain.autoTurn(-angleToPerpendicular);
				} else {
					Robot.drivetrain.autoTurn(angleToPerpendicular);
				}
				stateCommandSent = true;
			}
			
			if(!Robot.drivetrain.isInPosition()) {
				break;
			} else {
				state = State.ALIGN_X;
			}
		case ALIGN_X:
			if(!stateCommandSent) {
				Robot.drivetrain.autoDrive(0, distanceToAlign);
			}
			
			if(!Robot.drivetrain.isInPosition()) {
				break;
			} else {
				state = State.TURN_TO_GOAL;
			}
		case TURN_TO_GOAL:
			if(!stateCommandSent) {
				if(rightOfGoal) {
					Robot.drivetrain.autoTurn(angleToPerpendicular);
				} else {
					Robot.drivetrain.autoTurn(-angleToPerpendicular);
				}
			}
			
			if(!Robot.drivetrain.isInPosition()) {
				break;
			} else {
				state = State.ALIGN_Y;
			}
			
		case ALIGN_Y:
			if(!stateCommandSent) {
				Robot.drivetrain.autoDrive(0, distanceToTarget);
			}
			
			if(!Robot.drivetrain.isInPosition()) {
				break;
			} else {
				state = State.FINISHED;
			}
		}
	}

	protected boolean isFinished() {
		return (state == State.FINISHED);
	}

	protected void end() {
	}

	protected void interrupted() {
	}
}
