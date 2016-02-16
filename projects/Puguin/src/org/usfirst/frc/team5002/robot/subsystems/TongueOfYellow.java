package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.command.Subsystem;
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

	public void initDefaultCommand() {
		// Set the default command for a subsystem here.
		// setDefaultCommand(new MySpecialCommand());
	}
}
