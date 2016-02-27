package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * Runs the wheels on the front of the arm that move the ball into the shooter.
 */
public class BarOfWheels extends Subsystem {
	private CANTalon BarSpinner;

	public BarOfWheels() {
		BarSpinner = new CANTalon(36);/* replace this with actual motor id */
		BarSpinner.changeControlMode(TalonControlMode.PercentVbus);
	}

	public void initDefaultCommand() {
	}

	/**
	 * run motor at max speed
	 */
	public void run() {
		BarSpinner.set(1.0);
	}

	/**
	 * run motor at specified speed
	 * @param percentspeed
	 */
	public void run(double percentspeed) {
		BarSpinner.set(percentspeed);

	}

	/**
	 * run motor backwards at max speed
	 */
	public void runbackwards() {
		BarSpinner.set(-1.0);
	}

	/**
	 * stops motor
	 */
	public void stop() {
		BarSpinner.set(0);
	}

	/**
	 * perform safety checks for motor 
	 * temperature
	 * @return
	 */
	public boolean isSafe() {
		if (BarSpinner.getTemperature() < 200) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * send data to the smartdashboard
	 */
	public void UpdateSD() {
		SmartDashboard.putNumber("BarSpinner get", BarSpinner.get());
		SmartDashboard.putNumber("BarSpinner OutputVoltage", BarSpinner.getOutputVoltage());
		SmartDashboard.putNumber("BarSpinner OutputCurrent", BarSpinner.getOutputCurrent());

	}

}
