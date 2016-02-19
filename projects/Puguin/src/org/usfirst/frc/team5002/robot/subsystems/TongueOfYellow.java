package org.usfirst.frc.team5002.robot.subsystems;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
/**
 * 
 *
 *
 */
public class TongueOfYellow extends Subsystem {
	CANTalon WinchMotor = new CANTalon(0);
	public TongueOfYellow() {
		WinchMotor = new CANTalon(0); // replace with actual motor id
		WinchMotor.changeControlMode(TalonControlMode.PercentVbus);
	}

	public void initDefaultCommand() {}

	public void run(double percentSpeed) {
		WinchMotor.set(percentSpeed);
	}

	public void stop() {
		WinchMotor.set(0);
	}

	public boolean isSafe() {
		if (WinchMotor.getTemperature() < 200) {
			return true;
		} else {
			return false;
		}
	}

	public void UpdateSD() {
		SmartDashboard.putNumber("WinchMotor get", WinchMotor.get());
		SmartDashboard.putNumber("WinchMotor OutputVoltage", WinchMotor.getOutputVoltage());
		SmartDashboard.putNumber("WinchMotor OutputCurrent", WinchMotor.getOutputCurrent());
	}
}
