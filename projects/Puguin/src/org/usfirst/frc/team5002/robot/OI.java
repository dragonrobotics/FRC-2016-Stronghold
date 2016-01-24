package org.usfirst.frc.team5002.robot;


import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.buttons.Button;
import edu.wpi.first.wpilibj.buttons.JoystickButton;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * This class is the glue that binds the controls on the physical operator
 * interface to the commands and command groups that allow control of the robot.
 */
public class OI {
	private Joystick XboxController;
	
	
public OI() {	
	 XboxController = new Joystick(0);
	 	XboxController.getRawAxis(1);
	 	XboxController.getRawAxis(2);
	 	
	
    Button X = new JoystickButton(XboxController, 1); //** I have not set the buttons to anything
    Button Y = new JoystickButton(XboxController, 2); // so they don't have a "name"
    Button B = new JoystickButton(XboxController, 3);
    Button A = new JoystickButton(XboxController, 4);
    Button Paddle_1 = new JoystickButton(XboxController, 5);
    Button Paddle_2 = new JoystickButton(XboxController, 6);
    Button Paddle_3 = new JoystickButton(XboxController, 7);
    Button Paddle_4 = new JoystickButton(XboxController, 8);
    Button RT = new JoystickButton(XboxController, 9);
    Button LT = new JoystickButton(XboxController, 10);
    Button RB = new JoystickButton(XboxController, 11);
    Button LB = new JoystickButton(XboxController, 12);
	
	    
	}
    //// CREATING BUTTONS
    // One type of button is a joystick button which is any button on a joystick.
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
    
    // Start the command when the button is released  and let it run the command
    // until it is finished as determined by it's isFinished method.
    // button.whenReleased(new ExampleCommand());
	public Joystick getJoystick(){
		return XboxController;
	
	}
}


