package org.usfirst.frc.team5002.robot;

import org.usfirst.frc.team5002.robot.commands.BeltMagic;
import org.usfirst.frc.team5002.robot.commands.BeltWizardry;
import org.usfirst.frc.team5002.robot.commands.ExampleCommand;
import org.usfirst.frc.team5002.robot.commands.LauncherMagic;

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
		pugstick.getRawAxis(3);
		pugstick.getRawAxis(4);
		pugstick.getRawAxis(5);
		
		
		Button X = new JoystickButton(pugstick, 3); 											
		Button Y = new JoystickButton(pugstick, 4); 
		Button B = new JoystickButton(pugstick, 2);
		Button A = new JoystickButton(pugstick, 1);
		Button L_Stick = new JoystickButton(pugstick,9);
		Button R_Stick = new JoystickButton(pugstick,10);
		Button RB = new JoystickButton(pugstick, 6);
		Button LB = new JoystickButton(pugstick, 5);
		

			
		X.whileHeld(new ExampleCommand());
		Y.whileHeld(new ExampleCommand());
		B.whileHeld(new ExampleCommand());
		A.whileHeld(new ExampleCommand());
		L_Stick.whenPressed(new ExampleCommand());
		R_Stick.whenPressed(new ExampleCommand());
		RB.whileHeld(new LauncherMagic());
		LB.whileHeld(new BeltWizardry()); }
		
		
	
		
		
	
	
	/*private Joystick bloop; {
	bloop = new Joystick(1);
	
	Button RT = new JoystickButton(bloop, 1);
	Button LT = new JoystickButton(bloop, 2);
	Button RB = new JoystickButton(bloop, 3);
	Button LB = new JoystickButton(bloop, 4);
	
	
	RT.whileHeld(new LauncherMagic());
	LT.whileHeld(new BeltMagic());
	RB.whileHeld(new LauncherMagic());
	LB.whileHeld(new BeltWizardry()); }
	*/
	
	
		
	
	
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
