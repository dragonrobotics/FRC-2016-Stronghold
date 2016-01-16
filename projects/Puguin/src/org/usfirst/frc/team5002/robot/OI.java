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
	private Joystick stick;
	;
	
public OI() {	
	stick = new Joystick(0);
	
    Button button1 = new JoystickButton(stick, 1); //** I have not set the buttons to anything
    Button button2 = new JoystickButton(stick, 2); // so they don't have a "name"
    Button button3 = new JoystickButton(stick, 3);
    Button button4 = new JoystickButton(stick, 4);
    Button button5 = new JoystickButton(stick, 5);
    Button button6 = new JoystickButton(stick, 6);
    Button button7 = new JoystickButton(stick, 7);
    Button button8 = new JoystickButton(stick, 8);
    Button button9 = new JoystickButton(stick, 9);
    Button button10 = new JoystickButton(stick, 10);
    Button button11 = new JoystickButton(stick, 11);
    Button button12 = new JoystickButton(stick, 12);
	
	    
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
}

