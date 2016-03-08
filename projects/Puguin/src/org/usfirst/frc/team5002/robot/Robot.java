package org.usfirst.frc.team5002.robot;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.SPI.Port;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import java.io.IOException;
import org.usfirst.frc.team5002.robot.commands.TriggerHappy;
import org.usfirst.frc.team5002.robot.subsystems.BarOfWheels;
import org.usfirst.frc.team5002.robot.subsystems.Belt;
import org.usfirst.frc.team5002.robot.subsystems.Drivetrain;
import org.usfirst.frc.team5002.robot.subsystems.Jetson;
import org.usfirst.frc.team5002.robot.subsystems.Launcher;
import org.usfirst.frc.team5002.robot.subsystems.Positioner;
import org.usfirst.frc.team5002.robot.subsystems.ThoseArmThings;
import org.usfirst.frc.team5002.robot.subsystems.TongueOfYellow;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {
	public static final Drivetrain drivetrain = new Drivetrain();
	public static final Launcher launcher = new Launcher();
	public static final Belt belt = new Belt();
	public static final ThoseArmThings thosearmthings = new ThoseArmThings();
	public static final TongueOfYellow tongueofyellow = new TongueOfYellow();
	public static final Positioner positioner = new Positioner();
	public static final BarOfWheels barofwheels = new BarOfWheels();
	public static Jetson jetson;
	public static OI oi;
	public static AHRS ahrs;
	
	Command autonomousCommand;
	CameraServer server;
	public Robot() {
		try {
			ahrs = new AHRS(Port.kMXP);
		} catch (RuntimeException ex) {
			DriverStation.reportError("Error instantiating navX MXP:  " + ex.getMessage(), true);
			ahrs = null;
		}
        server = CameraServer.getInstance();
        server.setQuality(50);
        //the camera name (ex "cam0") can be found through the roborio web interface
        server.startAutomaticCapture("cam0");
	}

	/**
	 * This function is run when the robot is first started up and should be
	 * used for any initialization code.
	 */
	public void robotInit() {
		oi = new OI();
		TriggerHappy trigger = new TriggerHappy();
		trigger.start();

		try {
			jetson = new Jetson();
			jetson.doDiscover(); // find the Jetson on the local network
		} catch (IOException e) {
			e.printStackTrace();
			// we can't recover from this, really.
			// I'm not really sure how to just kill the robot immediately.

			/*
			 * Chase Stockton: Yes we absolutely can recover from this. We don't
			 * need the jetson or vision processing to move the robot around or
			 * fire the ball. Just take proper precaution in anything that tries
			 * to access any feedback from the jetson (i.e. the command that
			 * fires the ball).
			 * 
			 * More specifically, make a method in this class that returns
			 * feedback from the jetson. If the jetson wasn't found, then throw
			 * an IllegalStateException or something and let the caller worry
			 * about handling the exception. Don't allow outside access to the
			 * jetson in any manner other than that method.
			 */
		}
	}

	public void disabledPeriodic() {
		Scheduler.getInstance().run();
	}

	public void autonomousInit() {
		drivetrain.initAutonomous();

		if (autonomousCommand != null)
			autonomousCommand.start();
	}
	
	/**
	 * This function is called periodically during autonomous
	 */
	public void autonomousPeriodic() {
		Scheduler.getInstance().run();
		oi.updateSD();
		positioner.updateFromAccelerometer();
		positioner.updateFromOdometry((drivetrain.getLVel() + drivetrain.getRVel()) / 2);
		
		// do asynch recv
		try {
			jetson.checkForMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void teleopInit() {
		if (autonomousCommand != null)
			autonomousCommand.cancel();

		drivetrain.initTeleop();
	}

	/**
	 * This function is called when the disabled button is hit. You can use it
	 * to reset subsystems before shutting down.
	 */
	public void disabledInit() {}
	/**
	 * This function is called periodically during operator control
	 */
	public void teleopPeriodic() {
		Scheduler.getInstance().run();
		oi.updateSD();
		positioner.updateFromAccelerometer();
		positioner.updateFromOdometry((drivetrain.getLVel() + drivetrain.getRVel()) / 2);
		
		try {
			jetson.checkForMessage();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This function is called periodically during test mode
	 */
	public void testPeriodic() {
		LiveWindow.run();
	}

	/**
	 * Get the current accumulated heading of the robot; when the robot is started up, this will be zero.
	 * Clockwise turns make this value increase, while counterclockwise turns make this decrease.
	 * This angle is continuous-- its range is beyond 360.
	 * @return accumulated heading displacement from robot start value.
	 * @throws IllegalStateException if the AHRS has not been initialized.
	 */
	public static double getRobotAngle() throws IllegalStateException {
		if (ahrs == null) {
			throw new IllegalStateException("AHRS not initialized.");
		}
		return ahrs.getAngle();
	}
}
