package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;

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
	// Put methods for controlling this subsystem
	// here. Call these from Commands.

	public void initDefaultCommand() {
		// Set the default command for a subsystem here.

		// setDefaultCommand(new MySpecialCommand());
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
		}

		else {
			return false;
		}

	}

	public void stop() {
		// TODO Auto-generated method stub
		leftArm.set(0);
		rightArm.set(0);
	}
}
