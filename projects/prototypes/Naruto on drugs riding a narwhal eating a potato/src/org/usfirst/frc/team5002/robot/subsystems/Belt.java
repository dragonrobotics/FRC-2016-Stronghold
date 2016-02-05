package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The belt subsystem. For this version of the prototype, there are 2 belts each
 * with their own motor and TalonSRX driving them.
 */
public class Belt extends Subsystem {
	private CANTalon mcL, mcR;
	private DigitalInput ballSwitch;

	/**
	 * <p>
	 * The constructor. Called when a new instance of this class is made.
	 * </p>
	 * <p>
	 * Initializes the CANTalon variables and sets initial settings.
	 * </p>
	 */
	public Belt() {
		mcL = new CANTalon(2);
		mcR = new CANTalon(5);
		
		ballSwitch = new DigitalInput(9);

		mcL.changeControlMode(TalonControlMode.PercentVbus);
		mcR.changeControlMode(TalonControlMode.PercentVbus);
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
	 * Gets the state of the limit switch used to detect the ball on the belt.
	 * @return The state of the limit switch. True = pressed, false otherwise.
	 */
	public boolean getBallSwitch() {
		return !ballSwitch.get();
	}
	
	/**
	 * Updates the smart dashboard with values from this subsystem.
	 */
	public void updateSD() {
		SmartDashboard.putNumber("beltL", mcL.get());
		SmartDashboard.putNumber("beltR", mcR.get());
		SmartDashboard.putBoolean("beltBallSwitch", getBallSwitch());
	}
}
