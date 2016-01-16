package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.command.Subsystem;

/**
 *
 */
public class Drivetrain extends Subsystem {
	CANTalon mc1, mc2, mc3, mc4, mc5, mc6;

	// Put methods for controlling this subsystem
	// here. Call these from Commands.
	public Drivetrain() {

	}

	public void initDefaultCommand() {
		// Set the default command for a subsystem here.
		// setDefaultCommand(new MySpecialCommand());
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