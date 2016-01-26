package org.usfirst.frc.team5002.robot.subsystems;

import org.usfirst.frc.team5002.robot.commands.LauncherDefault;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;

/**
 * 
 */
public class Launcher extends Subsystem {
    private CANTalon leftLaunchWheel, rightLaunchWheel;
    boolean leftReverse = false;
    boolean rightReverse = true;
    
    public Launcher() {
    	leftLaunchWheel = new CANTalon(2); /* TODO: Replace this with the actual motor id */
    	rightLaunchWheel = new CANTalon(1);
    	
    	leftLaunchWheel.changeControlMode(TalonControlMode.PercentVbus);
    	rightLaunchWheel.changeControlMode(TalonControlMode.PercentVbus);
    }
    
    /***
     * Run Launcher motors at max speed.
     */
    public void run() {
    	leftLaunchWheel.set(1.0 * (leftReverse ? -1.0 : 1.0));
    	rightLaunchWheel.set(1.0 * (rightReverse ? -1.0 : 1.0));
    }
    
    /***
     * Run Launcher motors at max speed in reverse direction.
     */
    public void runBackwards() {
    	leftLaunchWheel.set(-1.0 * (leftReverse ? -1.0 : 1.0));
    	rightLaunchWheel.set(-1.0 * (rightReverse ? -1.0 : 1.0));
    }
    
    /***
     * Run launcher motors at specified speed.
     * 
     * @param speed Speed to run motors at, range: -1.0 to 1.0 (max backwards to max forwards). See motor set() method.
     */
    public void run(double speed) {
    	leftLaunchWheel.set(speed * (leftReverse ? -1.0 : 1.0));
    	rightLaunchWheel.set(speed * (rightReverse ? -1.0 : 1.0));
    }
    
    /***
     * Stop launcher motors.
     */
    public void stop() {
    	leftLaunchWheel.set(0);
    	rightLaunchWheel.set(0);
    }
    
    /***
     * Invert all outputs to motor.
     */
    public void reverseMotorDirection() {
    	leftReverse = !leftReverse;
    	rightReverse = !rightReverse;
    }

    public void initDefaultCommand() {
    	setDefaultCommand(new LauncherDefault());
    }
}

