package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;


import edu.wpi.first.wpilibj.command.Command;

/**
 *
 */
public class TriggerHappy extends Command {
	private Command FireBalls;
	private Command GetBalls;
		

    public TriggerHappy() {
    	FireBalls = new DoLaunch();
    	GetBalls = new BeltMagic();
     
    }

    // Called just before this Command runs the first time
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
	protected void execute() {
		if (Robot.oi.getJoystick().getRawAxis(3) == -1) {
			FireBalls.start();
		}
		if (Robot.oi.getJoystick().getRawAxis(3) == 1) {
			GetBalls.start();
		}
	}

    // Make this return true when this Command no longer needs to run execute()
    protected boolean isFinished() {
        return false;
    }

    // Called once after isFinished returns true
    protected void end() {
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    protected void interrupted() {
    }
}
