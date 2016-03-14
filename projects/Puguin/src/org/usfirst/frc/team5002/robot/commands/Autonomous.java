package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Command;

/**
 * Another autonomous logic command.
 * 
 * TODO: Is currently very minimal-- only continually runs the robot diagonally forever.
 */
public class Autonomous extends Command {
	private double sec = 0;
	private double time;
	public Autonomous(double time) {
		requires(Robot.drivetrain);
		this.time = time;
	}

	protected void initialize() {
//		sec = Timer.getFPGATimestamp();
//		Robot.drivetrain.zeroMotors();
	}

	protected void execute() {
//		Robot.drivetrain.moveForward(3000);
//		if(Robot.drivetrain.getError() > 50){
//			sec = Timer.getFPGATimestamp();
//		}
		Robot.drivetrain.moveForward(1);
	}

	protected boolean isFinished() {
//		return Timer.getFPGATimestamp() - sec > 2;
		return this.timeSinceInitialized() > time;
	}
	

	protected void end() {
		Robot.drivetrain.stop();
	}

	protected void interrupted() {
		end();
	}
}
