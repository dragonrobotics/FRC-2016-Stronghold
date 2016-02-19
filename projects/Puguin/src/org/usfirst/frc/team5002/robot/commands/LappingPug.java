package org.usfirst.frc.team5002.robot.commands;
import org.usfirst.frc.team5002.robot.Robot;
import edu.wpi.first.wpilibj.command.Command;
/**
 *This summons the Pug god, a mystical being of immense metaphysical power
 */
public class LappingPug extends Command {
    public LappingPug() {
    	requires(Robot.tongueofyellow);
     }

     protected void initialize() {
    }

     protected void execute() {
    	Robot.tongueofyellow.run(-1.0);
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
