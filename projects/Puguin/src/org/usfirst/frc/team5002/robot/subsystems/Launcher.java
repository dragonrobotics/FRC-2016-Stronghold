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
    
    public Launcher() {
    	leftLaunchWheel = new CANTalon(2); /* TODO: Replace this with the actual motor id */
    	rightLaunchWheel = new CANTalon(1);
    	
    	leftLaunchWheel.changeControlMode(TalonControlMode.PercentVbus);
    	rightLaunchWheel.changeControlMode(TalonControlMode.PercentVbus);
    }
    
    public void run() {
    	leftLaunchWheel.set(1.0);
    	rightLaunchWheel.set(-1.0);
    }
    
    public void run(double speed) {
    	leftLaunchWheel.set(speed);
    	rightLaunchWheel.set(-speed);
    }
    
    public void stop() {
    	leftLaunchWheel.set(0);
    	rightLaunchWheel.set(0);
    }

    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand(new MySpecialCommand());
    	setDefaultCommand(new LauncherDefault());
    }
}

