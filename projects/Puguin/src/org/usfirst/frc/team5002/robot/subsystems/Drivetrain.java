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
 *
 */
public class Drivetrain extends Subsystem {
	private CANTalon mc1, mc2, mc3, mc4, mc5, mc6;

	/**
	 * constructor for drivetrain initializes CANTalon stuff
	 */
	public Drivetrain() {
		mc1 = new CANTalon(31);
		mc2 = new CANTalon(32);
		mc3 = new CANTalon(33);
		mc4 = new CANTalon(34);
		mc5 = new CANTalon(35);
		mc6 = new CANTalon(36);

		mc1.changeControlMode(TalonControlMode.Position);		
		mc2.changeControlMode(TalonControlMode.Follower);
		mc3.changeControlMode(TalonControlMode.Follower);
		mc4.changeControlMode(TalonControlMode.Position);		
		mc5.changeControlMode(TalonControlMode.Follower);
		mc6.changeControlMode(TalonControlMode.Follower);

		mc1.setFeedbackDevice(FeedbackDevice.QuadEncoder);
		mc4.setFeedbackDevice(FeedbackDevice.QuadEncoder);
		
		mc1.setPID(1.0, 0, 0);
		mc4.setPID(1.0, 0, 0);
		
		mc2.set(1);
		mc3.set(1);
		mc5.set(4);
		mc6.set(4);
	}

	public void initDefaultCommand() {
		 setDefaultCommand(new TeleopDriveyWivey());
	}
	
	public void initTeleop() {
		mc1.changeControlMode(TalonControlMode.Speed);
		mc4.changeControlMode(TalonControlMode.Speed);
	}
	
	public void initAutonomous() {
		mc1.changeControlMode(TalonControlMode.Position);
		mc4.changeControlMode(TalonControlMode.Position);
	}

	public void joystickDrive(Joystick stick) {
		mc1.set(stick.getY() - stick.getX());
		mc4.set(stick.getY() + stick.getX());
		 
	} 

	public void joystickFOCDrive(Joystick stick) {
		Robot.getRobotAngle();
		double PugAngle = Robot.getRobotAngle();
		double JoystickAngle = stick.getDirectionDegrees();
		
		if (JoystickAngle < PugAngle){
			mc1.set(1);
			mc4.set(-1);
		}
		else if(JoystickAngle > PugAngle){
			mc1.set(-1);
			mc4.set(1);
		}
		else{
			mc1.set(stick.getMagnitude());
			mc4.set(stick.getMagnitude());
		}

	}

	public void autoDrive(double x, double y, double angle) {

	}

	public boolean isInPosition() {

		return false;
	}

	public void stop() {
		mc1.set(0);
		mc4.set(0);
	}

	public boolean isSafe() {

		if (mc1.getTemperature() < 200 && mc4.getTemperature() < 200) {
			return true;
		}

		else {
			return false;
		}

	}
}
