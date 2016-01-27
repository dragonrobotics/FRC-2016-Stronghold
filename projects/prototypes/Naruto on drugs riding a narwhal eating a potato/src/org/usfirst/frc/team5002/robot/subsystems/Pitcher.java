package org.usfirst.frc.team5002.robot.subsystems;

import org.usfirst.frc.team5002.robot.commands.PugstickAttack;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.ControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

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
		mcL = new CANTalon(2);
		mcR = new CANTalon(1);

		mcL.changeControlMode(ControlMode.PercentVbus);
		mcR.changeControlMode(ControlMode.PercentVbus);
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
	 */
	public void set(double speed, double spin) {
		// prevents both wheels from spinning backwards and tearing up the ball.
		if (speed < 0) {
			speed = 0;
		}
		// makes sure a value greater than 1 isn't passed to the set method.
		if (speed + Math.abs(spin) > 1) {
			speed = 1 - Math.abs(spin);
		}
		
		mcL.set(spin + speed);
		mcR.set(spin - speed);
	}
	
	/**
	 * Updates the smart dashboard with values from this subsystem.
	 */
	public void updateSD() {
		SmartDashboard.putNumber("spinL", mcL.get());
		SmartDashboard.putNumber("spinR", mcR.get());
	}
}
