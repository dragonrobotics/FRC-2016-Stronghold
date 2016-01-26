package org.usfirst.frc.team5002.robot;

import org.usfirst.frc.team5002.robot.commands.FireKoala;
import org.usfirst.frc.team5002.robot.commands.SetCookieProduction;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.buttons.Button;
import edu.wpi.first.wpilibj.buttons.JoystickButton;

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
		 * initialization of the joystick I'm overriding getY() to make it
		 * actually return -getY() this is a shortcut to override (or add)
		 * methods to newly created objects
		 */
		pugstick = new Joystick(0) {
			@Override
			public double getY(Hand hand) {
				return -super.getY(hand);
			}
		};

		/*
		 * Declaration & initialization of buttons
		 * snowmobile = slow
		 * jetski =fast
		 * F = forward
		 * R = reverse
		 */
		Button snowmobileF = new JoystickButton(pugstick, 3);
		Button jetskiF = new JoystickButton(pugstick, 5);
		Button snowmobileR = new JoystickButton(pugstick, 4);
		Button jetskiR = new JoystickButton(pugstick, 6);
		Button angryKoala = new JoystickButton(pugstick, 1);

		snowmobileF.whileHeld(new SetCookieProduction(0.2));
		jetskiF.whileHeld(new SetCookieProduction(1.0));
		snowmobileR.whileHeld(new SetCookieProduction(-0.2));
		jetskiR.whileHeld(new SetCookieProduction(-0.5));
		angryKoala.toggleWhenPressed(new FireKoala());
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
	}
}
