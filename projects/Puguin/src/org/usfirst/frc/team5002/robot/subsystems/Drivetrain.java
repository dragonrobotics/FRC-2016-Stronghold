package org.usfirst.frc.team5002.robot.subsystems;

import org.usfirst.frc.team5002.robot.Robot;
import org.usfirst.frc.team5002.robot.commands.TeleopDriveyWivey;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.CANTalon.FeedbackDevice;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Joystick.RumbleType;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Robot drivetrain.
 * 
 * The drivetrain is a tracked slipdrive system with three motors per side.
 * Two motors have encoders and are used for automatic drive measurement.
 */
public class Drivetrain extends Subsystem {
	/** 
	 * Motor objects onboard the robot.
	 * L = Left, R = Right, B = Back, F = Forward.
	 * Motors LT and RT are used as auto driving targets.
	 */
	private CANTalon mcLT, mcLB, mcLF, mcRT, mcRB, mcRF;
	private double sec = 0;
	
	private static final double maxSpeed = 400;
//	private static final double maxSpeed = 1;
	/**
	 * The drivetrain ignores angle changes less than this value, 
	 * to prevent it from constantly turning without moving forward.
	 */
	
	public Drivetrain() {
		mcLT = new CANTalon(6);
		mcLB = new CANTalon(10);
		mcLF = new CANTalon(11);
		mcRT = new CANTalon(9);
		mcRB = new CANTalon(8);
		mcRF = new CANTalon(5);

//		mcLT.changeControlMode(TalonControlMode.PercentVbus); //TODO: Fix the encoder on the drivetrain and set Position and Speed PID values. Also make the drivetrain run more smoothly
//		mcRT.changeControlMode(TalonControlMode.PercentVbus);
		mcLT.changeControlMode(TalonControlMode.Position);
		mcLB.changeControlMode(TalonControlMode.Follower);
		mcLF.changeControlMode(TalonControlMode.Follower);
		mcRT.changeControlMode(TalonControlMode.Position);
		mcRB.changeControlMode(TalonControlMode.Follower);
		mcRF.changeControlMode(TalonControlMode.Follower);

		mcLT.setFeedbackDevice(FeedbackDevice.QuadEncoder);
		mcRT.setFeedbackDevice(FeedbackDevice.QuadEncoder);

		mcLB.set(mcLT.getDeviceID());
		mcLF.set(mcLT.getDeviceID());
		mcRB.set(mcRT.getDeviceID());
		mcRF.set(mcRT.getDeviceID());
	}

	/**
	 * Sets default command for the drivetrain (teleop mode)
	 */
	public void initDefaultCommand() {
		setDefaultCommand(new TeleopDriveyWivey());
	}

	/**
	 * Set motor values directly from a joystick object.
	 * Call from teleop code.
	 * @param stick -- Joystick to use for driving.
	 */
	public void joystickDrive(Joystick stick) {
		checkControlMode(TalonControlMode.Speed);
		try{
			if(Robot.getRobotRoll() > 45) {
				stick.setRumble(RumbleType.kLeftRumble, (float)(Math.min(60-Robot.getRobotRoll(),15))/15);
			}
		} catch(IllegalStateException e) {}
		
		double y = stick.getY(),
				x = stick.getX()/(1.2+3*Math.abs(y));
			
		if (Math.abs(x) + Math.abs(y) > 1) {
			x /= Math.abs(x)+Math.abs(y);
			y /= Math.abs(x)+Math.abs(y);
		}
		
		mcLT.set(-maxSpeed / (stick.getRawAxis(2)*3+1) * (stick.getY() - stick.getX()));
		mcRT.set(maxSpeed / (stick.getRawAxis(2)*3+1) * (stick.getY() + stick.getX()));

	}

	/**
	 * Drive the robot in Field-Oriented Control mode.
	 * @param stick Joystick to use for driving.
	 */
	public void joystickFOCDrive(Joystick stick) {
		checkControlMode(TalonControlMode.Speed);
		Robot.getRobotYaw();
		double PugAngle = Robot.getRobotYaw();
		double JoystickAngle = stick.getDirectionDegrees();
		if (JoystickAngle < PugAngle) {
			mcLT.set(1);
			mcRT.set(-1);
		} else if (JoystickAngle > PugAngle) {
			mcLT.set(-1);
			mcRT.set(1);
		} else {
			mcLT.set(stick.getMagnitude());
			mcRT.set(stick.getMagnitude());
		}

	}
	
	/**
	 * Move the robot a given distance relative to its current position.
	 * 
	 * @param x Distance to drive on the x-axis
	 * @param y Distance to drive on the y-axis
	 */
	public void autoDrive(double x, double y) {
		checkControlMode(TalonControlMode.Position);
		double initangle = Math.atan2(x , y); // Angle to the final position
		double initdistance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)); 
		// Distance
		// directly
		// to
	    // the
		// final
	    // position

		autoTurn(initangle);

		mcLT.set(initdistance);
		mcRT.set(initdistance);
	}
	
	/***
	 * autoTurn -- turn to a given angle change in degrees.
	 * Heading zero corresponds to straight ahead.
	 * @param hdg heading change in degrees. Positive values correspond to clockwise rotation.
	 */
	final double maxTurnOutput = 100.0;
	
	public void autoTurn(double hdg) {
		checkControlMode(TalonControlMode.Position);
		double startAngle = Robot.getRobotYaw();
		double endAngle = startAngle + hdg;
		
		mcLT.changeControlMode(TalonControlMode.Speed);
		mcRT.changeControlMode(TalonControlMode.Speed);
		
		while(true) {
			double curErr = endAngle - Robot.getRobotYaw();
			
			if(Math.abs(curErr) < 1) {
				break;
			}
			
			curErr = Math.min(curErr, 100);
			
			double out = (curErr / 100) * maxTurnOutput;
			
			if(hdg > 0) {
				mcLT.set(out);
				mcRT.set(-out);
			} else {
				mcLT.set(-out);
				mcRT.set(out);
			}
		}
		
		mcLT.changeControlMode(TalonControlMode.Position);
		mcRT.changeControlMode(TalonControlMode.Position);
	}
	
	public void moveForward(double pos){
		checkControlMode(TalonControlMode.Position);
		
		double robotang = 0;
		try{
			if(Robot.getRobotRoll() > 60) {
				mcLT.set(-999999999);
				mcRT.set(999999999);
				return;
			}
			robotang = Robot.getRobotYaw();
			robotang = Math.min(10 * robotang, 200);
		} finally {
			mcLT.set(pos + robotang);
			mcRT.set(pos - robotang);
		}
		mcLT.set(pos);
		mcRT.set(-pos); 
	}
	public void zeroMotors(){
		mcLT.setEncPosition(0);
		mcRT.setEncPosition(0);
	}
	
	
	/**
	 * Get current turning speed of the left-side tracks.
	 * @return encoder-measured robot speed on left side.
	 */
	public int getLVel() {
		return mcLT.getEncVelocity();
	}
	
	/**
	 * Get current turning speed of the right-side tracks.
	 * @return encoder-measured robot speed on right side.
	 */
	public int getRVel() {
		return mcRT.getEncVelocity();
	}

	/**
	 * Test if both the left and right tracks are in position after an auto-drive command.
	 * @return whether or not the left and right tracks have completed an auto-drive command
	 */
	public double getError() {
		return mcLT.getClosedLoopError() + mcRT.getClosedLoopError();
	}
	
	public void checkControlMode(TalonControlMode mode){
		if (mcLT.getControlMode() != mode){
			mcLT.changeControlMode(mode);
		}
		if (mcRT.getControlMode() != mode){
			mcRT.changeControlMode(mode);
		}
		if (mode == TalonControlMode.Speed){
			mcLT.setProfile(0);
			mcRT.setProfile(0);
		}
		if (mode == TalonControlMode.Position){
			mcLT.setProfile(1);
			mcRT.setProfile(1);
		}
	}

	/**
	 * Immediately stop the left and right tracks.
	 */
	public void stop() {
		mcLT.set(0);
		mcRT.set(0);
	}

	/**
	 * Test if motor temperatures are within acceptable limits.
	 * @return if motor temperatures are within safe limits.
	 */
	public boolean isSafe() {

		return mcLT.getTemperature() < 200 && mcRT.getTemperature() < 200; 

	}

	/**
	 * Update the Smart Dashboard with drivetrain debugging information.
	 */
	public void updateSD() {
		SmartDashboard.putNumber("drivetrain.mcLT.Speed", mcLT.getSpeed());
		SmartDashboard.putNumber("drivetrain.mcRT.Speed", mcRT.getSpeed());
		SmartDashboard.putNumber("drivetrain.mcLT.Error", mcLT.getError());
		SmartDashboard.putNumber("drivetrain.mcRT.Error", mcRT.getError());
		SmartDashboard.putNumber("drivetrain.mcLT.Current", mcLT.getOutputCurrent());
		SmartDashboard.putNumber("drivetrain.mcRT.Current", mcRT.getOutputCurrent());
	}
}
