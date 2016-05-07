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
	private double argument;
	private boolean isEncoderHappy;
	public Autonomous(double argument) {
		this.argument = argument;
		requires(Robot.drivetrain);
	}

	protected void initialize() {
//		sec = Timer.getFPGATimestamp();
//		Robot.drivetrain.zeroMotors(); 
		isEncoderHappy = Robot.drivetrain.areEncodersWorking();
		if (isEncoderHappy) {
			this.setTimeout(5);
			Robot.drivetrain.zeroMotors();
		}
		else {
			this.setTimeout(argument/5000);
		}
	}

	protected void execute() {
		if (isEncoderHappy){
			Robot.drivetrain.moveForward(argument);
		}
		else{
			Robot.drivetrain.moveForward();
		}
	}

	protected boolean isFinished() {
		return this.isTimedOut();
//		return this.timeSinceInitialized() > time;
	}
	

	protected void end() {
		Robot.drivetrain.stop();
	}

	protected void interrupted() {
		end();
	}
}
