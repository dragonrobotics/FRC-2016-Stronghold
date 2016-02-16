package org.usfirst.frc.team5002.robot.subsystems;



import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 *
 */
public class Belt extends Subsystem {
	private CANTalon leftBelt;

    
    public Belt() {
    	leftBelt = new CANTalon(7); /* TODO: Replace this with the actual motor id */
    	
    	leftBelt.changeControlMode(TalonControlMode.PercentVbus);
    }
    
    public void initDefaultCommand() {}
    /*** 
     * Run belt motors at max speed.
     */
    public void run() {
    	leftBelt.set(-.4);
    }
    
    /***
     * Run belt motors at specified speed.
     * 
     * @param percentSpeed Speed to run motors at, range: -1.0 to 1.0 (max backwards to max forwards). See motor set() method.
     */
    public void run(double percentSpeed) {
    	leftBelt.set(percentSpeed);
    }
    

    /*** 
     * Run belt motors at max speed in reverse direction.
     */
    public void runBackwards() {
    	leftBelt.set(.4);
    }
    

    /***
     * Stop both belt motors.
     */
    public void stop() {
    	leftBelt.set(0);
    }

	public boolean isSafe(){
		
		if (leftBelt.getTemperature() < 200) {			
			return true;
		}
		
		else{	
			return false;
		}
		
	}
	
	public void updateSD() {
		SmartDashboard.putNumber("belt get", leftBelt.get());
		SmartDashboard.putNumber("belt temp", leftBelt.getTemperature());
		SmartDashboard.putNumber("belt current", leftBelt.getOutputCurrent());
		SmartDashboard.putNumber("belt voltage", leftBelt.getOutputVoltage());
	}
}


