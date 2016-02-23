package org.usfirst.frc.team5002.robot;

import org.usfirst.frc.team5002.robot.commands.BeltMagic;
import org.usfirst.frc.team5002.robot.commands.BeltWizardry;
import org.usfirst.frc.team5002.robot.commands.LappingPug;
import org.usfirst.frc.team5002.robot.commands.LauncherMagic;
import org.usfirst.frc.team5002.robot.commands.PugKisses;
import org.usfirst.frc.team5002.robot.commands.TOUCHDOWN;
import org.usfirst.frc.team5002.robot.commands.WhatAreThose;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Joystick.RumbleType;
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
		pugstick.getRawAxis(3);
		pugstick.setRumble(RumbleType.kLeftRumble, 1); 
		
//		Button X = new JoystickButton(pugstick, 3); 											
		Button Y = new JoystickButton(pugstick, 4); 
		Button B = new JoystickButton(pugstick, 2);
		Button A = new JoystickButton(pugstick, 1);
		Button Paddle_2 = new JoystickButton(pugstick,9);
		Button Paddle_4 = new JoystickButton(pugstick,10);
		Button RB = new JoystickButton(pugstick, 6);
		Button LB = new JoystickButton(pugstick, 5);
		
//		X.whileHeld(new ());
		Y.whileHeld(new PugKisses());
		B.whileHeld(new BeltMagic());
		A.whileHeld(new LappingPug());
		Paddle_2.whenPressed(new TOUCHDOWN());
		Paddle_4.whenPressed(new WhatAreThose());
		RB.whileHeld(new LauncherMagic());
		LB.whileHeld(new BeltWizardry());
	}
	
	public Joystick getJoystick() {
		return pugstick;
	}
	
	public void updateSD(){
		Robot.belt.updateSD();
		Robot.drivetrain.updateSD();
		Robot.launcher.updateSD();
		SmartDashboard.putNumber("POV", pugstick.getPOV());
		Robot.tongueofyellow.UpdateSD();
		Robot.thosearmthings.UpdateSD();
	}
}
