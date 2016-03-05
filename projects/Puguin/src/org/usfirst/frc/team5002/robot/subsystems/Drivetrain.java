package org.usfirst.frc.team5002.robot.subsystems;

import org.usfirst.frc.team5002.robot.Robot;
import org.usfirst.frc.team5002.robot.commands.TeleopDriveyWivey;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.CANTalon.FeedbackDevice;
import edu.wpi.first.wpilibj.Joystick;
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

	/**
	 * The drivetrain ignores angle changes less than this value, 
	 * to prevent it from constantly turning without moving forward.
	 */
	static final controllerDeadzone = 2.5;
	
	/**
	 * constructor for drivetrain initializes CANTalon stuff
	 */
	public Drivetrain() {
		mcLT = new CANTalon(6);
		mcLB = new CANTalon(10);
		mcLF = new CANTalon(11);
		mcRT = new CANTalon(9);
		mcRB = new CANTalon(8);
		mcRF = new CANTalon(5);

		mcLT.changeControlMode(TalonControlMode.Position);
		mcLB.changeControlMode(TalonControlMode.Follower);
		mcLF.changeControlMode(TalonControlMode.Follower);
		mcRT.changeControlMode(TalonControlMode.Position);
		mcRB.changeControlMode(TalonControlMode.Follower);
		mcRF.changeControlMode(TalonControlMode.Follower);

		mcLT.setFeedbackDevice(FeedbackDevice.QuadEncoder);
		mcRT.setFeedbackDevice(FeedbackDevice.QuadEncoder);

		mcLT.setPID(1.0, 0, 0);
		mcRT.setPID(1.0, 0, 0);

		mcLB.set(1);
		mcLF.set(1);
		mcRB.set(4);
		mcRF.set(4);
	}

	/**
	 * Sets default command for the drivetrain (teleop mode)
	 */
	public void initDefaultCommand() {
		setDefaultCommand(new TeleopDriveyWivey());
	}

	/**
	 * Prepares motor controllers to accept values for teleop driving (speed)
	 */
	public void initTeleop() {
		mcLT.changeControlMode(TalonControlMode.Speed);
		mcRT.changeControlMode(TalonControlMode.Speed);
	}

	/**
	 * Prepares motor controllers to accept values for auto driving (position / distance)
	 */
	public void initAutonomous() {
		mcLT.changeControlMode(TalonControlMode.Position);
		mcRT.changeControlMode(TalonControlMode.Position);
	}

	/**
	 * Set motor values directly from a joystick object.
	 * Call from teleop code.
	 * @param stick -- Joystick to use for driving.
	 */
	public void joystickDrive(Joystick stick) {
		autoDrive(stick.getX(), stick.getX());
	}

	/**
	 * Drive the robot in Field-Oriented Control mode.
	 * @param stick Joystick to use for driving.
	 */
	public void joystickFOCDrive(Joystick stick) {
		Robot.getRobotAngle();
		double PugAngle = Robot.getRobotAngle() % 360.0;
		double JoystickAngle = stick.getDirectionDegrees();

		if (Math.abs(PugAngle-JoystickAngle) > controllerDeadzone) {
			autoTurn(JoystickAngle - PugAngle);
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
		double initangle = Math.atan2(y, x); // Angle to the final position
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
	 * Maximum speed while turning.
	 */
	final double maxTurnOutput = 100.0;
	
	/***
	 * turn to a given angle change in degrees.
	 *
	 * Heading zero corresponds to straight ahead.
	 * @param hdg heading change in degrees. Positive values correspond to clockwise rotation.
	 */
	public void autoTurn(double hdg) {
		autoTurnAbsolute(Robot.getRobotAngle() + hdg);
	}
	
	/***
	 * turn to an absolute robot angle in degrees.
	 *
	 * Note that angles here are continuous: heading = (Angle % 360.0).
	 * @param hdg heading change in degrees. Positive values correspond to clockwise rotation.
	 */
	public void autoTurnAbsolute(double endAngle) {
		mcLT.changeControlMode(TalonControlMode.Speed);
		mcRT.changeControlMode(TalonControlMode.Speed);
		
		while(true) {
			double curErr = endAngle - Robot.getRobotAngle();
			
			if(Math.abs(curErr) < 1) {
				break;
			}
			
			if(Math.abs(curErr) > 100)
				curErr = 100 * (curErr < 0 ? -1 : 1);
			
			double out = (Math.abs(curErr) / 100) * maxTurnOutput;
			
			if(curErr > 0) {
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
	public boolean isInPosition() {
		return mcLT.getClosedLoopError() + mcRT.getClosedLoopError() < 50;
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

		if (mcLT.getTemperature() < 200 && mcRT.getTemperature() < 200) {
			return true;
		}

		else {
			return false;
		}

	}

	/**
	 * Update the Smart Dashboard with drivetrain debugging information.
	 */
	public void updateSD() {
		SmartDashboard.putNumber("mc1 get", mcLT.get());
		SmartDashboard.putNumber("mc2 get", mcLB.get());
		SmartDashboard.putNumber("mc3 get", mcLF.get());
		SmartDashboard.putNumber("mc4 get", mcRT.get());
		SmartDashboard.putNumber("mc5 get", mcRB.get());
		SmartDashboard.putNumber("mc6 get", mcRF.get());
		SmartDashboard.putNumber("mc1 BusVoltage", mcLT.getBusVoltage());
		SmartDashboard.putNumber("mc2 BusVoltage", mcLB.getBusVoltage());
		SmartDashboard.putNumber("mc3 BusVoltage", mcLF.getBusVoltage());
		SmartDashboard.putNumber("mc4 BusVoltage", mcRT.getBusVoltage());
		SmartDashboard.putNumber("mc5 BusVoltage", mcRB.getBusVoltage());
		SmartDashboard.putNumber("mc6 BusVoltage", mcRF.getBusVoltage());
		SmartDashboard.putNumber("mc1 ClosedLoopError", mcLT.getClosedLoopError());
		SmartDashboard.putNumber("mc2 ClosedLoopError", mcLB.getClosedLoopError());
		SmartDashboard.putNumber("mc3 ClosedLoopError", mcLF.getClosedLoopError());
		SmartDashboard.putNumber("mc4 ClosedLoopError", mcRT.getClosedLoopError());
		SmartDashboard.putNumber("mc5 ClosedLoopError", mcRB.getClosedLoopError());
		SmartDashboard.putNumber("mc6 ClosedLoopError", mcRF.getClosedLoopError());
		SmartDashboard.putNumber("mc1 OutputVoltage", mcLT.getOutputVoltage());
		SmartDashboard.putNumber("mc2 OutputVoltage", mcLB.getOutputVoltage());
		SmartDashboard.putNumber("mc3 OutputVoltage", mcLF.getOutputVoltage());
		SmartDashboard.putNumber("mc4 OutputVoltage", mcRT.getOutputVoltage());
		SmartDashboard.putNumber("mc5 OutputVoltage", mcRB.getOutputVoltage());
		SmartDashboard.putNumber("mc6 OutputVoltage", mcRF.getOutputVoltage());
		SmartDashboard.putNumber("mc1 OutputCurrent", mcLT.getOutputCurrent());
		SmartDashboard.putNumber("mc2 OutputCurrent", mcLB.getOutputCurrent());
		SmartDashboard.putNumber("mc3 OutputCurrent", mcLF.getOutputCurrent());
		SmartDashboard.putNumber("mc4 OutputCurrent", mcRT.getOutputCurrent());
		SmartDashboard.putNumber("mc5 OutputCurrent", mcRB.getOutputCurrent());
		SmartDashboard.putNumber("mc6 OutputCurrent", mcRF.getOutputCurrent());

	}
}
