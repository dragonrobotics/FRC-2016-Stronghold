package org.usfirst.frc.team5002.robot.subsystems;

import org.usfirst.frc.team5002.robot.commands.PugstickAttack;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.CANTalon.FeedbackDevice;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/*
 * .735 == base power
 */

/**
 * The pitcher subsystem. In this prototype, there are 2 wheels horizontal to
 * each other that shoot the ball out. The ball is fed into them via the belt
 * subsystem.
 */
public class Pitcher extends Subsystem {
	private CANTalon mcL, mcR;

	/**
	 * <p>
	 * The constructor. Called when a new instance of this class is made.
	 * </p>
	 * <p>
	 * Initializes the CANTalon variables and sets initial settings.
	 * </p>
	 */
	public Pitcher() {
		mcL = new CANTalon(3);
		mcR = new CANTalon(4);

//		mcL.changeControlMode(ControlMode.PercentVbus);
//		mcR.changeControlMode(ControlMode.PercentVbus);

		mcL.setFeedbackDevice(FeedbackDevice.QuadEncoder);
		mcR.setFeedbackDevice(FeedbackDevice.QuadEncoder);
		
		mcL.changeControlMode(TalonControlMode.Speed);
		mcR.changeControlMode(TalonControlMode.Speed);
		
		mcL.setPID(0.1, 0.000, 0, 0.1, 100, 50, 0);
		mcR.setPID(0.1, 0.000, 0, 0.1, 100, 50, 0);
	}

	public void initDefaultCommand() {
		/*
		 * when no other command that requires the subsystem is currently
		 * running, this command will be run.
		 */
		setDefaultCommand(new PugstickAttack());
	}

	/**
	 * Sets the wheels to run at a certain speed.
	 * 
	 * @param speed - the base speed the motors run at. Acceptable values
	 * 				  are between -1 and 1.
	 * 
	 * @param spin - differs the motors' speeds by this amount, effectively
	 *               applying "spin" to the ball. Acceptable values are
	 *               between -1 and 1.
	 *               
	 * @param spread - determines how much the motors decrease/increase their
	 * 				   speed as a result of the spin. -1 = decrease only one
	 * 				   motor, 0 = decrease/increase both motors, 1 = increase
	 * 				   only one motor.
	 */
	public void set(double speed, double spin, double spread) {
		// prevents both wheels from spinning backwards and tearing up the ball.
		double speedL,speedR;
		if (speed < 0) {
			speed = 0;
		}
		if (speed + Math.abs(spin) + Math.abs(spread)*Math.abs(spin) > 1) {
			speed = 1 - Math.abs(spin) + Math.abs(spread)*Math.abs(spin);
		}
		
		speedL = speed + (spin+spread*spin)/2;
		speedR = speed - (spin-spread*spin)/2;

		mcL.set(1000*speedL);
		mcR.set(1000*speedR);
	}
	
	/**
	 * Sets the wheels to run at a certain speed.
	 * 
	 * @param speed - the base speed the motors run at. Acceptable values
	 * 				  are between -1 and 1.
	 * 
	 * @param spin - differs the motors' speeds by this amount, effectively
	 *               applying "spin" to the ball. Acceptable values are
	 *               between -1 and 1.
	 */
	public void set(double speed, double spin) {
		set(speed, spin, 0);
	}
	
	/**
	 * Updates the smart dashboard with values from this subsystem.
	 */
	public void updateSD() {
		SmartDashboard.putNumber("spinL", mcL.get());
		SmartDashboard.putNumber("spinR", mcR.get());
		
		SmartDashboard.putNumber("spinL_Set", mcL.getSetpoint());
		SmartDashboard.putNumber("spinR_Set", mcR.getSetpoint());

		SmartDashboard.putNumber("spinL_Err", mcL.getClosedLoopError());
		SmartDashboard.putNumber("spinR_Err", mcR.getClosedLoopError());
		
		SmartDashboard.putNumber("spinL_V", mcL.getOutputVoltage());
		SmartDashboard.putNumber("spinR_V", mcR.getOutputVoltage());
		
		SmartDashboard.putNumber("spinL_C", mcL.getOutputCurrent());
		SmartDashboard.putNumber("spinR_C", mcR.getOutputCurrent());
	}
}
