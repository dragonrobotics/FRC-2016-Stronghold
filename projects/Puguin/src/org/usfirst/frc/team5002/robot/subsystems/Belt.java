package org.usfirst.frc.team5002.robot.subsystems;



import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;

/**
 *
 */
public class Belt extends Subsystem {
	private CANTalon leftBelt, rightBelt;

    
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
    	leftBelt.set(.2);
    	rightBelt.set(-.2);
    }
    
    /***
     * Run belt motors at specified speed.
     * 
     * @param percentSpeed Speed to run motors at, range: -1.0 to 1.0 (max backwards to max forwards). See motor set() method.
     */
    public void run(double percentSpeed) {
    	leftBelt.set(percentSpeed);
    	rightBelt.set(percentSpeed);
    }
    
    /*** 
     * Run belt motors at max speed in reverse direction.
     */
    public void runBackwards() {
    	leftBelt.set(-.2);
    	rightBelt.set(.2);
    }
    

    /***
     * Stop both belt motors.
     */
    public void stop() {
    	leftBelt.set(0);
    	rightBelt.set(0);
    }

    public void initDefaultCommand() {
    }
}


