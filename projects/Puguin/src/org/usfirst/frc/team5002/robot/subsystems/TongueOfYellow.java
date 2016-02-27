package org.usfirst.frc.team5002.robot.subsystems;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
/**
 * Lifting winch thing(?)
 */
public class TongueOfYellow extends Subsystem {
	CANTalon WinchMotor = new CANTalon(36);
	public TongueOfYellow() {
		WinchMotor = new CANTalon(36); // replace with actual motor id
		WinchMotor.changeControlMode(TalonControlMode.PercentVbus);
	}

	public void initDefaultCommand() {}

	/**
	 * Run the winch motor.
	 * @param percentSpeed speed to run winch motor at.
	 */
	public void run(double percentSpeed) {
		WinchMotor.set(percentSpeed);
	}

	/**
	 * Immediately stop the winch motor.
	 */
	public void stop() {
		WinchMotor.set(0);
	}

	/**
	 * Check if motors are running at safe temperatures.
	 * @return whether or not the motor temperatures are safe.
	 */
	public boolean isSafe() {
		if (WinchMotor.getTemperature() < 200) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Send debugging info to the Smart Dashboard.
	 */
	public void UpdateSD() {
		SmartDashboard.putNumber("WinchMotor get", WinchMotor.get());
		SmartDashboard.putNumber("WinchMotor OutputVoltage", WinchMotor.getOutputVoltage());
		SmartDashboard.putNumber("WinchMotor OutputCurrent", WinchMotor.getOutputCurrent());
	}
}
