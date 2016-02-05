
package org.usfirst.frc.team5002.robot;

import edu.wpi.first.wpilibj.IterativeRobot;
//import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;

import org.usfirst.frc.team5002.robot.commands.FireKoala;
import org.usfirst.frc.team5002.robot.subsystems.Belt;
import org.usfirst.frc.team5002.robot.subsystems.Pitcher;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {

	/*
	 * Declaration and initialization of subsystems. static lets you access
	 * these just using the class name (don't have to make an object) final
	 * makes it so that you can't change these (safety)
	 */
	public static final Belt belt = new Belt();
	public static final Pitcher pitcher = new Pitcher();
	/*
	 * Declaration of the variable that holds the OI object.
	 */
	public static OI oi;
//	private static double lastTime = 0;

	Command autonomousCommand;

	/**
	 * This function is run when the robot is first started up and should be
	 * used for any initialization code.
	 */
	public void robotInit() {
		/*
		 * the oi MUST be initialized after subsystems: the oi maps commands to
		 * buttons. Commands require subsystems. If the subsystems don't exist,
		 * that's a problem.
		 */
		oi = new OI();

		// initialize the command used for the autonomous period
		// autonomousCommand = new ExampleCommand();
	}

	/**
	 * This method is called periodically while the robot is disabled.
	 */
	public void disabledPeriodic() {
		Scheduler.getInstance().run();
		Robot.oi.updateSD();
	}

	private FireKoala fk = new FireKoala();
	/**
	 * The method called when autonomous mode is first enabled.
	 */
	public void autonomousInit() {
		// schedule the autonomous command (example)
		if (autonomousCommand != null) {
			autonomousCommand.start();
		}
		fk.cancel();
		fk.start();
	}

	/**
	 * This method is called periodically while autonomous is enabled.
	 */
	public void autonomousPeriodic() {
		Scheduler.getInstance().run();
		oi.updateSD();
//		if (fk.isRunning()) {
//			lastTime = Timer.getFPGATimestamp();
//		} else if (Timer.getFPGATimestamp() - lastTime > 4) {
//			fk.start();
//		}
	}

	/**
	 * The method called when teleop mode is first enabled.
	 */
	public void teleopInit() {
		/*
		 * This makes sure that the autonomous stops running when teleop starts
		 * running. If you want the autonomous to continue until interrupted by
		 * another command, remove these lines or comment them out.
		 */
		if (autonomousCommand != null)
			autonomousCommand.cancel();
	}

	/**
	 * This method is called periodically while teleop is enabled.
	 */
	public void teleopPeriodic() {
		Scheduler.getInstance().run();
		Robot.oi.updateSD();
	}

	/**
	 * This method is called periodically while test mode is enabled.
	 */
	public void testPeriodic() {
		LiveWindow.run();
	}
}
