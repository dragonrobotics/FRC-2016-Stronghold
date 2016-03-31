package org.usfirst.frc.team5002.robot;

import org.usfirst.frc.team5002.robot.commands.BeltMagic;
import org.usfirst.frc.team5002.robot.commands.BeltWizardry;
import org.usfirst.frc.team5002.robot.commands.FranksSleehw;
import org.usfirst.frc.team5002.robot.commands.FranksWheels;
import org.usfirst.frc.team5002.robot.commands.LappingPug;
import org.usfirst.frc.team5002.robot.commands.LauncherMagic;
import org.usfirst.frc.team5002.robot.commands.PugKisses;
import org.usfirst.frc.team5002.robot.commands.BipolarCamera;
import org.usfirst.frc.team5002.robot.commands.DoLaunch;
import org.usfirst.frc.team5002.robot.commands.TOUCHDOWN;
import org.usfirst.frc.team5002.robot.commands.WhatAreThose;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Joystick.AxisType;
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
	/**
	 * This creates the Joystick and sets the Axis,
	 * and allows the left trigger to rumble.
	 */

	public OI() {
		pugstick = new Joystick(0) {
			@Override
			public double getRawAxis(int axis) {
				double r = super.getRawAxis(axis);
				r = Math.abs(r) < 0.1 ? 0 : Math.signum(r) * (Math.abs(r) - 0.1) / 0.9;
				r = Math.signum(r) * Math.pow(Math.abs(r), 3);
				return r;
			}
		};
		pugstick.setAxisChannel(AxisType.kX,4);
		pugstick.setAxisChannel(AxisType.kY,5);
		/**
		 * Sets the buttons on the remote.
		 */
		Button X = new JoystickButton(pugstick, 3);
//		Button Y = new JoystickButton(pugstick, 4);
		Button B = new JoystickButton(pugstick, 2);
//		Button A = new JoystickButton(pugstick, 1);
		Button Paddle_2 = new JoystickButton(pugstick, 9);
		Button Paddle_4 = new JoystickButton(pugstick, 10);
		Button RB = new JoystickButton(pugstick, 6);
		Button LB = new JoystickButton(pugstick, 5);


		/**
		 * when buttons: X, Y, B, A, RB, LB are held down,
		 * the code is activated. When the Paddles are
		 * pressed, the code is activated.
		 */
//		X.whenPressed(new BipolarCamera());
//		Y.whileHeld(new PugKisses());
		B.whileHeld(new BeltMagic());
		B.whileHeld(new FranksSleehw()); //Can you even use one button for two commands?
//		A.whileHeld(new LappingPug());
		Paddle_2.whileHeld(new TOUCHDOWN());
		Paddle_4.whileHeld(new WhatAreThose());
		RB.whileHeld(new DoLaunch());
		LB.whenPressed(new BeltWizardry());
		LB.whenPressed(new FranksWheels());

	}

	/**
	 * Keeps the code continually active
	 * @return
	 */
	public Joystick getJoystick() {
		return pugstick;
	}

	/**
	 * Sends all of the data collected in the subsystems
	 * to the smartdashboard.
	 */
	public void updateSD() {
		Robot.belt.updateSD();
		Robot.drivetrain.updateSD();
		Robot.launcher.updateSD();
		SmartDashboard.putNumber("POV", pugstick.getPOV());
//		Robot.tongueofyellow.UpdateSD();
		Robot.thosearmthings.UpdateSD();
		Robot.barofwheels.UpdateSD();
		Robot.jetson.UpdateSD();
		SmartDashboard.putNumber("robot.yaw", Robot.getRobotYaw());
		SmartDashboard.putNumber("robot.roll", Robot.getRobotRoll());
		SmartDashboard.putNumber("robot.pitch", Robot.getRobotPitch());


	}
}
