package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.ControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The belt subsystem. For this version of the prototype, there are 2 belts each
 * with their own motor and TalonSRX driving them.
 */
public class Belt extends Subsystem {
	private CANTalon mcL, mcR;

	/**
	 * <p>
	 * The constructor. Called when a new instance of this class is made.
	 * </p>
	 * <p>
	 * Initializes the CANTalon variables and sets initial settings.
	 * </p>
	 */
	public Belt() {
		mcL = new CANTalon(3);
		mcR = new CANTalon(4);

		mcL.changeControlMode(ControlMode.PercentVbus);
		mcR.changeControlMode(ControlMode.PercentVbus);
	}

	public void initDefaultCommand() {

	}

	/**
	 * Sets the belts to run at a certain speed. Positive values are forward.
	 * 
	 * @param speed - the speed to set the motors to. Acceptable values are
	 *            between -1 and 1.
	 */
	public void set(double speed) {
		mcL.set(speed);
		mcR.set(-speed);
	}
	
	/**
	 * Updates the smart dashboard with values from this subsystem.
	 */
	public void updateSD() {
		SmartDashboard.putNumber("beltL", mcL.get());
		SmartDashboard.putNumber("beltR", mcR.get());
	}
}
