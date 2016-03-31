package org.usfirst.frc.team5002.robot.commands;

import org.usfirst.frc.team5002.robot.Robot;
import org.usfirst.frc.team5002.robot.subsystems.Jetson;
import edu.wpi.first.wpilibj.Timer;

/**
 * Fire with control.
 */
public class WhitesOfTheirEyes extends Command {
    private final double idealDistance = 4.0; // feet
    private final double distTolerance = 0.5; // +/-

    private final double idealAngle = 0.0; // degrees off centerline
    private final double angleTolerance = 30.0; // +/-

    private int state = 0;
    private double tS;

    public WhitesOfTheirEyes() {
		requires(Robot.launcher);
	}

	protected void initialize() {}

	protected void execute() {
        if(state == 0) {
            if(Robot.jetson.getGoalStatus() &&
                (Math.abs(Robot.jetson.getDistance() - idealDistance) < distTolerance) &&
                (Math.abs(Robot.jetson.getAngle()) < distTolerance)) {
                        state = 1;
                        tS = Timer.getFPGATimestamp();
                }
        }

        if(state == 1) {
            Robot.launcher.run(.75);
            if((Timer.getFPGATimestamp() - tS) > 1) {
                    state = 2;
                    Robot.launcher.stop();
            }
        }

        if(state == 2) {
			Robot.belt.run();
        }
	}

	protected boolean isFinished() {
		return (state == 2);
	}

	protected void end() {
		Robot.belt.stop();
        Robot.launcher.stop();
	}

	protected void interrupted() {
		end();
	}
}
