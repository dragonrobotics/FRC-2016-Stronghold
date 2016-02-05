package org.usfirst.frc.team5002.robot.subsystems;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.command.Subsystem;

/**
 * 
 */
public class Launcher extends Subsystem {
    private CANTalon leftLaunchWheel, rightLaunchWheel;
    private DigitalInput ballswitch;
    public Launcher() {
    	leftLaunchWheel = new CANTalon(3); /* TODO: Replace this with the actual motor id */
    	rightLaunchWheel = new CANTalon(4);
    	ballswitch=new DigitalInput(0); //TODO: Replace this with the actual port
    	leftLaunchWheel.changeControlMode(TalonControlMode.PercentVbus);
    	rightLaunchWheel.changeControlMode(TalonControlMode.PercentVbus);
    }
    
    /**
     * Ball on switch or na.
     * @return state of the switch
     */
    public boolean getballswitch(){
    	return ballswitch.get();
    }
    
    /***
     * Run Launcher motors at max speed.
     */
    public void run() {
    	leftLaunchWheel.set(-.567);
    	rightLaunchWheel.set(.567);
    }
    
    
    /***
     * Run launcher motors at specified speed.
     * 
     * @param speed Speed to run motors at, range: -1.0 to 1.0 (max backwards to max forwards). See motor set() method.
     */
    public void run(double speed) {
    	leftLaunchWheel.set(speed);
    	rightLaunchWheel.set(speed);
    }
    
    /***
     * Stop launcher motors.
     */
    public void stop() {
    	leftLaunchWheel.set(0);
    	rightLaunchWheel.set(0);
    }
    

    public void initDefaultCommand() {
    }
	public boolean isSafe(){
		
		if (leftLaunchWheel.getTemperature() < 200 && rightLaunchWheel.getTemperature() < 200) {			
			return true;
		}
		
		else{	
			return false;
		}
	}
}

