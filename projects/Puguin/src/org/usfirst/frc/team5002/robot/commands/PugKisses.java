package org.usfirst.frc.team5002.robot.commands;
import edu.wpi.first.wpilibj.command.Command;
import org.usfirst.frc.team5002.robot.Robot;
/**
 *makes tape measure extend
 */
public class PugKisses extends Command {
    public PugKisses() {
    	requires(Robot.tongueofyellow);
    }

    protected void initialize() {
    }

    protected void execute() {
    	Robot.tongueofyellow.run(1.0);
    }

    protected boolean isFinished() {
        return false;
    }

    protected void end() {
    	Robot.tongueofyellow.stop();
    }

    protected void interrupted() {
    	end();
    }
}
