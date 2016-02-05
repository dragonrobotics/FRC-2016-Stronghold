package org.usfirst.frc.team5002.robot;

import org.usfirst.frc.team5002.robot.commands.AcquireSentience;
import org.usfirst.frc.team5002.robot.commands.FireKoala;
import org.usfirst.frc.team5002.robot.commands.RestrainKoala;
import org.usfirst.frc.team5002.robot.commands.SetCookieProduction;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.buttons.Button;
import edu.wpi.first.wpilibj.buttons.JoystickButton;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * This class contains everything that the human player uses to interface with
 * the robot.
 */
public class OI {
	/**
	 * The joystick plugged into the copmuter.
	 */
	private Joystick pugstick;

	/**
	 * <p>
	 * The constructor. Called when a new instance of this class is made.
	 * </p>
	 * <p>
	 * Initializes the joystick, creates buttons, and sets actions of those
	 * buttons.
	 * </p>
	 */
	public OI() {
		/*
		 * initialization of the joystick
		 * I'm overriding getY() to make it actually return -getY()
		 * this is an example of a shortcut to override (or add) methods to
		 * newly created objects
		 */
		pugstick = new Joystick(0);

		Button launchKoala = new JoystickButton(pugstick,1);
		Button restrainKoala = new JoystickButton(pugstick, 7);
		Button acquireSentience = new JoystickButton(pugstick, 2);
		Button snowmobileF = new JoystickButton(pugstick, 3);
		Button jetskiF = new JoystickButton(pugstick, 5);
		Button snowmobileR = new JoystickButton(pugstick, 4);
		Button jetskiR = new JoystickButton(pugstick, 6);
		
		launchKoala.whenPressed(new FireKoala());
		restrainKoala.toggleWhenPressed(new RestrainKoala());
		acquireSentience.whenPressed(new AcquireSentience());
		snowmobileF.whileHeld(new SetCookieProduction(0.2));
		jetskiF.whileHeld(new SetCookieProduction(1.0));
		snowmobileR.whileHeld(new SetCookieProduction(-0.2));
		jetskiR.whileHeld(new SetCookieProduction(-0.5));
	}

	/**
	 * Gets the joystick managed by the OI.
	 * 
	 * @return Joystick - the joystick the driver is holding
	 */
	public Joystick getJoystick() {
		return pugstick;
	}

	/**
	 * Globally updates the SmartDashboard with values from all subsystems.
	 */
	public void updateSD() {
		Robot.belt.updateSD();
		Robot.pitcher.updateSD();
		SmartDashboard.putNumber("stick_Throttle",pugstick.getThrottle());
	}
}
