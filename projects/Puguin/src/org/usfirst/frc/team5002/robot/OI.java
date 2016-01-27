package org.usfirst.frc.team5002.robot;

import org.usfirst.frc.team5002.robot.commands.BeltMagic;
import org.usfirst.frc.team5002.robot.commands.BeltWizardry;
import org.usfirst.frc.team5002.robot.commands.DoLaunch;
import org.usfirst.frc.team5002.robot.commands.ExampleCommand;


import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.buttons.Button;
import edu.wpi.first.wpilibj.buttons.JoystickButton;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * This class is the glue that binds the controls on the physical operator
 * interface to the commands and command groups that allow control of the robot.
 */
public class OI {
	private Joystick pugstick;

	public OI() {
		pugstick = new Joystick(0);
		pugstick.getRawAxis(1);
		pugstick.getRawAxis(2);

		Button X = new JoystickButton(pugstick, 1); // ** I have not set the
													// buttons to anything
		Button Y = new JoystickButton(pugstick, 2); // so they don't have a
													// "name"
		Button B = new JoystickButton(pugstick, 3);
		Button A = new JoystickButton(pugstick, 4);
		Button Paddle_1 = new JoystickButton(pugstick, 5);
		Button Paddle_2 = new JoystickButton(pugstick, 6);
		Button Paddle_3 = new JoystickButton(pugstick, 7);
		Button Paddle_4 = new JoystickButton(pugstick, 8);
		Button RT = new JoystickButton(pugstick, 9);
		Button LT = new JoystickButton(pugstick, 10);
		Button RB = new JoystickButton(pugstick, 11);
		Button LB = new JoystickButton(pugstick, 12);

		B.whileHeld(new ExampleCommand());
		A.whileHeld(new ExampleCommand());
		Paddle_1.whileHeld(new ExampleCommand());
		Paddle_2.whileHeld(new ExampleCommand());
		Paddle_3.whileHeld(new ExampleCommand());
		Paddle_4.whileHeld(new ExampleCommand());
		RT.whileHeld(new DoLaunch());
		LT.whileHeld(new BeltMagic());
		RB.whileHeld(new DoLaunch());
		LB.whileHeld(new BeltWizardry());
		
		
	}
	//// CREATING BUTTONS
	// One type of button is a joystick button which is any button on a
	//// joystick.
	// You create one by telling it which joystick it's on and which button
	// number it is.
	// Joystick stick = new Joystick(port);
	// Button button = new JoystickButton(stick, buttonNumber);

	// There are a few additional built in buttons you can use. Additionally,
	// by subclassing Button you can create custom triggers and bind those to
	// commands the same as any other Button.

	//// TRIGGERING COMMANDS WITH BUTTONS
	// Once you have a button, it's trivial to bind it to a button in one of
	// three ways:

	// Start the command when the button is pressed and let it run the command
	// until it is finished as determined by it's isFinished method.
	// button.whenPressed(new ExampleCommand());

	// Run the command while the button is being held down and interrupt it once
	// the button is released.
	// button.whileHeld(new ExampleCommand());

	// Start the command when the button is released and let it run the command
	// until it is finished as determined by it's isFinished method.
	// button.whenReleased(new ExampleCommand());
	public Joystick getJoystick() {
		return pugstick;

	}
}
