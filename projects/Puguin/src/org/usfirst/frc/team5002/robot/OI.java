package org.usfirst.frc.team5002.robot;

import org.usfirst.frc.team5002.robot.commands.BeltMagic;
import org.usfirst.frc.team5002.robot.commands.BeltWizardry;
import org.usfirst.frc.team5002.robot.commands.LauncherMagic;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.buttons.Button;
import edu.wpi.first.wpilibj.buttons.JoystickButton;

/**
 * This class is the glue that binds the controls on the physical operator
 * interface to the commands and command groups that allow control of the robot.
 */
public class OI {
	private Joystick pugstick;

	public OI() {
		pugstick = new Joystick(0);
		
		Button X = new JoystickButton(pugstick, 3); 											
		Button Y = new JoystickButton(pugstick, 4); 
		Button B = new JoystickButton(pugstick, 2);
		Button A = new JoystickButton(pugstick, 1);
		Button L_Stick = new JoystickButton(pugstick,9);
		Button R_Stick = new JoystickButton(pugstick,10);
		Button RB = new JoystickButton(pugstick, 6);
		Button LB = new JoystickButton(pugstick, 5);
		
//		X.whileHeld(new ExampleCommand());
//		Y.whileHeld(new ExampleCommand());
//		B.whileHeld(new ExampleCommand());
//		A.whileHeld(new ExampleCommand());
//		L_Stick.whenPressed(new ExampleCommand());
//		R_Stick.whenPressed(new ExampleCommand());
		RB.whileHeld(new LauncherMagic());
		LB.whileHeld(new BeltWizardry());
		A.whileHeld(new BeltMagic());
	}
		

	
	public Joystick getJoystick() {
		return pugstick;

	}
	
	public void updateSD(){
		Robot.belt.updateSD();
		Robot.drivetrain.updateSD();
		Robot.launcher.updateSD();
	}
}
