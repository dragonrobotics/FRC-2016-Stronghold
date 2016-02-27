package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 *
 */
public class Belt extends Subsystem {
	private CANTalon onlyBelt;

	public Belt() {
		onlyBelt = new CANTalon(14); /* TODO: Replace this with the actual motor id */
		onlyBelt.changeControlMode(TalonControlMode.PercentVbus);
		
	}

	public void initDefaultCommand() {
	}

	/***
	 * Run belt motors backwards at 40% speed
	 */
	public void run() {
		onlyBelt.set(-.4);
	}

	/***
	 * Run belt motors at specified speed.
	 * 
	 * @param percentSpeed
	 *            Speed to run motors at, range: -1.0 to 1.0 (max backwards to
	 *            max forwards). See motor set() method.
	 */
	public void run(double percentSpeed) {
		onlyBelt.set(percentSpeed);
	}

	/***
	 * Run belt motors at max speed in reverse direction.
	 */
	public void runBackwards() {
		onlyBelt.set(.4);
	}

	/***
	 * Stop both belt motors.
	 */
	public void stop() {
		onlyBelt.set(0);
	}

	/**
	 * Perform safety checks for motor temperature.
	 * 
	 * @return boolean -- are motor temperatures within acceptable bounds?
	 */
	public boolean isSafe() {
		if (onlyBelt.getTemperature() < 200) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * send data to the smartdashboard
	 */
	public void updateSD() {
		SmartDashboard.putNumber("belt get", onlyBelt.get());
		SmartDashboard.putNumber("belt temp", onlyBelt.getTemperature());
		SmartDashboard.putNumber("belt current", onlyBelt.getOutputCurrent());
		SmartDashboard.putNumber("belt voltage", onlyBelt.getOutputVoltage());
	}
}
