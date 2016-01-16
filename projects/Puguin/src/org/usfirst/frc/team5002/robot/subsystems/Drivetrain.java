package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
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
