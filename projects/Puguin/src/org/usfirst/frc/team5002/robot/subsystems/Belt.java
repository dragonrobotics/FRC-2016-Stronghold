package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 *
 */
public class Belt extends Subsystem {
	private CANTalon onlyBelt;
	private DigitalInput ballswitch;

	public Belt() {
		onlyBelt = new CANTalon(14);
		onlyBelt.changeControlMode(TalonControlMode.PercentVbus);
		
		ballswitch = new DigitalInput(0); // TODO: Replace this with the actual
		// port
	}

	public void initDefaultCommand() {
	}
	
	public boolean getballswitch() {
		return ballswitch.get();
	}

	/***
	 * Run belt motors backwards at 40% speed
	 */
	public void run() {
		onlyBelt.set(1.0);
	}

	/***
	 * Run belt motors at max speed in reverse direction.
	 */
	public void runBackwards() {
		onlyBelt.set(-1.0);
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
		SmartDashboard.putBoolean("belt.switch.get", ballswitch.get());
	}
}
