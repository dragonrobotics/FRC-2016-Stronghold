
package org.usfirst.frc.team5002.robot;

import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.SPI.Port;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import org.usfirst.frc.team5002.robot.commands.ExampleCommand;
import org.usfirst.frc.team5002.robot.commands.TriggerHappy;
import org.usfirst.frc.team5002.robot.subsystems.Belt;
import org.usfirst.frc.team5002.robot.subsystems.Drivetrain;
import org.usfirst.frc.team5002.robot.subsystems.Launcher;

import com.kauailabs.navx.frc.AHRS;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {
	
	//	public static final ExampleSubsystem exampleSubsystem = new ExampleSubsystem();
	public static final Drivetrain drivetrain = new Drivetrain();
	public static final Launcher launcher = new Launcher();
	public static final Belt belt = new Belt();
	public static OI oi;
	public static AHRS ahrs;

    Command autonomousCommand;

    public Robot() {
    	 try {   
    		 ahrs = new AHRS(Port.kMXP); 
         } catch (RuntimeException ex ) {
             DriverStation.reportError("Error instantiating navX MXP:  " + ex.getMessage(), true);
             ahrs = null;
         }
    	
		
	}
    
    /** 
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    public void robotInit() {
		oi = new OI();
        // instantiate the command used for the autonomous period
        autonomousCommand = new ExampleCommand();
        TriggerHappy trigger = new TriggerHappy();
        trigger.start();
      
    }
	
	public void disabledPeriodic() {
		Scheduler.getInstance().run();
	}

    public void autonomousInit() {
    	drivetrain.initAutonomous();
    	
        // schedule the autonomous command (example)
        if (autonomousCommand != null) autonomousCommand.start();
    }

    /**
     * This function is called periodically during autonomous
     */
    public void autonomousPeriodic() {
        Scheduler.getInstance().run();
    }

    public void teleopInit() {
		// This makes sure that the autonomous stops running when
        // teleop starts running. If you want the autonomous to 
        // continue until interrupted by another command, remove
        // this line or comment it out.
        if (autonomousCommand != null) autonomousCommand.cancel();
        
        drivetrain.initTeleop();
    }

    /**
     * This function is called when the disabled button is hit.
     * You can use it to reset subsystems before shutting down.
     */
    public void disabledInit(){

    }

    /**
     * This function is called periodically during operator control
     */
    public void teleopPeriodic() {
        Scheduler.getInstance().run();
    }
    
    /**
     * This function is called periodically during test mode
     */
    public void testPeriodic() {
        LiveWindow.run();
    }
    
    public static double getRobotAngle () throws IllegalStateException{
    	if (ahrs == null){
    		throw new IllegalStateException(); 
    	}
    	return ahrs.getAngle() ;
    	
    }
}
