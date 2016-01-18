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
    
    public Belt() {
    	leftBelt = new CANTalon(3); /* TODO: Replace this with the actual motor id */
    	rightBelt = new CANTalon(4);
    	
    	leftBelt.changeControlMode(TalonControlMode.PercentVbus);
    	rightBelt.changeControlMode(TalonControlMode.PercentVbus);
    }
    
    public void run() {
    	leftBelt.set(1.0);
    	rightBelt.set(-1.0);
    }
    
    public void run(double percentSpeed) {
    	leftBelt.set(percentSpeed);
    	rightBelt.set(percentSpeed);
    }
    
    public void stop() {
    	leftBelt.set(0);
    	rightBelt.set(0);
    }

    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand(new MySpecialCommand());
    	setDefaultCommand(new BeltDefault());
    }
}

