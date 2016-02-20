package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
*
*/
public class ThoseArmThings extends Subsystem {
	private CANTalon leftArm, rightArm;

	public ThoseArmThings() {
		leftArm = new CANTalon(8);
		rightArm = new CANTalon(9);
		leftArm.changeControlMode(TalonControlMode.PercentVbus);
		rightArm.changeControlMode(TalonControlMode.PercentVbus);
	}

	public void initDefaultCommand() {
	}

	public void armsup() {
		leftArm.set(1.0);
		rightArm.set(-1.0);
	}

	public void armsdown() {
		leftArm.set(-1.0);
		rightArm.set(1.0);

	}

	public boolean isSafe() {

		if (leftArm.getTemperature() < 200 && rightArm.getTemperature() < 200) {
			return true;
		} else {
			return false;
		}

	}

	public void stop() {
		leftArm.set(0);
		rightArm.set(0);
	}

	public void UpdateSD() {
		SmartDashboard.putNumber("leftArm get", leftArm.get());
		SmartDashboard.putNumber("rightArm get", rightArm.get());
		SmartDashboard.putNumber("leftArm OutputVoltage", leftArm.getOutputVoltage());
		SmartDashboard.putNumber("rightArm OutputVoltage", rightArm.getOutputVoltage());
		SmartDashboard.putNumber("leftArm OutputCurrent", leftArm.getOutputCurrent());
		SmartDashboard.putNumber("rightArm OutputCurrent", rightArm.getOutputCurrent());
	}

}
