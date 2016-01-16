package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.ControlMode;
import edu.wpi.first.wpilibj.CANTalon.FeedbackDevice;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.command.Subsystem;

/**
 *
 */
public class Drivetrain extends Subsystem {
	private CANTalon mc1, mc2, mc3, mc4, mc5, mc6;

	/**
	 * constructor for drivetrain initializes CANTalon stuff
	 */
	public Drivetrain() {
		mc1 = new CANTalon(1);
		mc2 = new CANTalon(2);
		mc3 = new CANTalon(3);
		mc4 = new CANTalon(4);
		mc5 = new CANTalon(5);
		mc6 = new CANTalon(6);

		mc1.changeControlMode(ControlMode.Position);		
		mc2.changeControlMode(ControlMode.Follower);
		mc3.changeControlMode(ControlMode.Follower);
		mc4.changeControlMode(ControlMode.Position);		
		mc5.changeControlMode(ControlMode.Follower);
		mc6.changeControlMode(ControlMode.Follower);

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
		// Set the default command for a subsystem here.
		// setDefaultCommand(new MySpecialCommand());
	}
	
	public void initTeleop() {
		mc1.changeControlMode(ControlMode.Speed);
		mc4.changeControlMode(ControlMode.Speed);
	}
	
	public void initAutonomous() {
		mc1.changeControlMode(ControlMode.Position);
		mc4.changeControlMode(ControlMode.Position);
	}

	public void joystickDrive(Joystick stick) {

	}

	public void joystickFOCDrive(Joystick stick) {

	}

	public void autoDrive(double x, double y, double angle) {

	}

	public boolean isInPosition() {

		return false;
	}
}
