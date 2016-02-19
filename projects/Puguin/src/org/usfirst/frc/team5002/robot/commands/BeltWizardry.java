package org.usfirst.frc.team5002.robot.commands;
import org.usfirst.frc.team5002.robot.Robot;
import edu.wpi.first.wpilibj.command.Command;
/**
 *
 */
public class BeltWizardry extends Command {
    public BeltWizardry() {
    	requires(Robot.belt);
    }

    protected void initialize() {
    }

    protected void execute() {
    	Robot.belt.run(1.0);
    }

    protected boolean isFinished() {
        return false;
    }

    protected void end() {
    	Robot.belt.stop();
    }

    protected void interrupted() {
    	end();
    }
}
