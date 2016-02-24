package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * 
 */
public class Launcher extends Subsystem {
	private CANTalon bottomLaunchWheel, toptLaunchWheel;
	private DigitalInput ballswitch;

	public Launcher() {
		bottomLaunchWheel = new CANTalon(12); /* TODO: Replace this with the actual motor id */
		toptLaunchWheel = new CANTalon(1);
		ballswitch = new DigitalInput(0); // TODO: Replace this with the actual
											// port
		bottomLaunchWheel.changeControlMode(TalonControlMode.PercentVbus);
		toptLaunchWheel.changeControlMode(TalonControlMode.PercentVbus);
	}

	/**
	 * Ball on switch or na.
	 * 
	 * @return state of the switch
	 */
	public boolean getballswitch() {
		return ballswitch.get();
	}

	/***
	 * Run Launcher motors at max speed.
	 */
	public void run() {
		bottomLaunchWheel.set(.75);
		toptLaunchWheel.set(.75);
	}

	/***
	 * Run launcher motors at specified speed.
	 * 
	 * @param speed
	 *            Speed to run motors at, range: -1.0 to 1.0 (max backwards to
	 *            max forwards). See motor set() method.
	 */
	public void run(double speed) {
		bottomLaunchWheel.set(speed);
		toptLaunchWheel.set(speed);
	}

	/***
	 * Stop launcher motors.
	 */
	public void stop() {
		bottomLaunchWheel.set(0);
		toptLaunchWheel.set(0);
	}

	public void initDefaultCommand() {
	}

	public boolean isSafe() {

		if (bottomLaunchWheel.getTemperature() < 200 && toptLaunchWheel.getTemperature() < 200) {
			return true;
		}

		else {
			return false;
		}
	}

	public void updateSD() {
		SmartDashboard.putNumber("LeftWheel get", bottomLaunchWheel.get());
		SmartDashboard.putNumber("RightWheel get", toptLaunchWheel.get());
		SmartDashboard.putNumber("LeftWheel BusVoltage", bottomLaunchWheel.getBusVoltage());
		SmartDashboard.putNumber("RightWheel BusVoltage", toptLaunchWheel.getBusVoltage());
		SmartDashboard.putNumber("LeftWheel OutputVoltage", bottomLaunchWheel.getOutputVoltage());
		SmartDashboard.putNumber("RightWheel OutputVoltage", toptLaunchWheel.getOutputVoltage());
		SmartDashboard.putNumber("LeftWheel OutputCurrent", bottomLaunchWheel.getOutputCurrent());
		SmartDashboard.putNumber("RightWheel OutputCurrent", toptLaunchWheel.getOutputCurrent());
		SmartDashboard.putNumber("LeftWheel ClosedLoopError", bottomLaunchWheel.getClosedLoopError());
		SmartDashboard.putNumber("RightWheel ClosedLooperror", toptLaunchWheel.getClosedLoopError());
	}
}
