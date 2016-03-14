package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Ball launcher and detector contact switch.
 */
public class Launcher extends Subsystem {
	private CANTalon bottomLaunchWheel, topLaunchWheel;
	private static final double maxSpeed = 17000;

	public Launcher() {
		bottomLaunchWheel = new CANTalon(12); /* TODO: Replace this with the actual motor id */
		topLaunchWheel = new CANTalon(1);
		
		bottomLaunchWheel.changeControlMode(TalonControlMode.Speed);
		topLaunchWheel.changeControlMode(TalonControlMode.Speed);
	}

	/***
	 * Run launcher motors at specified speed.
	 * 
	 * @param speed
	 *            Speed to run motors at, range: -1.0 to 1.0 (max backwards to
	 *            max forwards). See motor set() method.
	 */
	public void run(double speed) {
		checkControlMode(TalonControlMode.Speed);
		
		bottomLaunchWheel.set(-maxSpeed * speed);
		topLaunchWheel.set(-maxSpeed * speed);
	}

	/***
	 * Stop launcher motors.
	 */
	public void checkControlMode(TalonControlMode mode){
		if (bottomLaunchWheel.getControlMode() != mode){
			bottomLaunchWheel.changeControlMode(mode);
		}
		if (topLaunchWheel.getControlMode() != mode){
			bottomLaunchWheel.changeControlMode(mode);
		}
		
	}
	public void stop() {
		bottomLaunchWheel.set(0);
		topLaunchWheel.set(0);
	}

	public void initDefaultCommand() {
	}

	public boolean isSafe() {

		if (bottomLaunchWheel.getTemperature() < 200 && topLaunchWheel.getTemperature() < 200) {
			return true;
		}

		else {
			return false;
		}
	}

	public void updateSD() {
		SmartDashboard.putNumber("drivetrain.bottomLaunchWheel.Speed", bottomLaunchWheel.getSpeed());
		SmartDashboard.putNumber("drivetrain.topLaunchWheel.Speed", topLaunchWheel.getSpeed());
		SmartDashboard.putNumber("drivetrain.bottomLaunchWheel.Error", bottomLaunchWheel.getError());
		SmartDashboard.putNumber("drivetrain.topLaunchWheel.Error", topLaunchWheel.getError());
		SmartDashboard.putNumber("drivetrain.bottomLaunchWheel.Current", bottomLaunchWheel.getOutputCurrent());
		SmartDashboard.putNumber("drivetrain.topLaunchWheel.Current", topLaunchWheel.getOutputCurrent());
	}
}
