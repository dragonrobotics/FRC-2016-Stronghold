package org.usfirst.frc.team5002.robot.subsystems;

import org.usfirst.frc.team5002.robot.commands.BeltDefault;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;

/**
 *
 */
public class Belt extends Subsystem {
	private CANTalon leftBelt, rightBelt;
	boolean leftReverse = false;
    boolean rightReverse = true;
    
    public Belt() {
    	leftBelt = new CANTalon(3); /* TODO: Replace this with the actual motor id */
    	rightBelt = new CANTalon(4);
    	
    	leftBelt.changeControlMode(TalonControlMode.PercentVbus);
    	rightBelt.changeControlMode(TalonControlMode.PercentVbus);
    }
    
    /*** 
     * Run belt motors at max speed.
     */
    public void run() {
    	leftBelt.set(1.0 * (leftReverse ? -1.0 : 1.0));
    	rightBelt.set(1.0 * (rightReverse ? -1.0 : 1.0));
    }
    
    /***
     * Run belt motors at specified speed.
     * 
     * @param percentSpeed Speed to run motors at, range: -1.0 to 1.0 (max backwards to max forwards). See motor set() method.
     */
    public void run(double percentSpeed) {
    	leftBelt.set(percentSpeed * (leftReverse ? -1.0 : 1.0));
    	rightBelt.set(percentSpeed * (rightReverse ? -1.0 : 1.0));
    }
    
    /*** 
     * Run belt motors at max speed in reverse direction.
     */
    public void runBackwards() {
    	leftBelt.set(-1.0 * (leftReverse ? -1.0 : 1.0));
    	rightBelt.set(-1.0 * (rightReverse ? -1.0 : 1.0));
    }
    
    /***
     * Invert all outputs to motor.
     */
    public void reverseMotorDirection() {
    	leftReverse = !leftReverse;
    	rightReverse = !rightReverse;
    }
    
    /***
     * Stop both belt motors.
     */
    public void stop() {
    	leftBelt.set(0);
    	rightBelt.set(0);
    }

    public void initDefaultCommand() {
    	setDefaultCommand(new BeltDefault());
    }
}

