package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
* Super gate lifting arms.
*/
public class ThoseArmThings extends Subsystem {
	private CANTalon leftArm;

	public ThoseArmThings() {
		leftArm = new CANTalon(13);
	
		leftArm.changeControlMode(TalonControlMode.PercentVbus);
	
	}

	public void initDefaultCommand() {
	}

	/**
	 * Moves the arms up.
	 */
	public void armsup() {
		leftArm.set(.8);
	
	}

	/**
	 * Moves the arms down.
	 */
	public void armsdown() {
		leftArm.set(-0.4);


	}
	
	/**
	 * Safety check for the motors.
	 * 
	 * @return are the motor temps within safe values?
	 */
	public boolean isSafe() {
		if (leftArm.getTemperature() < 200 ) {
			
			return true;
		} else {
			return false;
		}

	}

	/**
	 * Stop all arm movement.
	 */
	public void stop() {
		leftArm.set(0);
	
	}

	/**
	 * Send debugging information to the Smart Dashboard.
	 */
	public void UpdateSD() {
		SmartDashboard.putNumber("leftArm get", leftArm.get());
		SmartDashboard.putNumber("leftArm OutputVoltage", leftArm.getOutputVoltage());
		SmartDashboard.putNumber("leftArm OutputCurrent", leftArm.getOutputCurrent());
	
	}

}
