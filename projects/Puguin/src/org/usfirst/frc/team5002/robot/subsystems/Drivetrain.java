package org.usfirst.frc.team5002.robot.subsystems;

import org.usfirst.frc.team5002.robot.Robot;
import org.usfirst.frc.team5002.robot.commands.TeleopDriveyWivey;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.CANTalon.TalonControlMode;
import edu.wpi.first.wpilibj.CANTalon.FeedbackDevice;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 *
 */
public class Drivetrain extends Subsystem {
	private CANTalon mcLT, mcLB, mcLF, mcRT, mcRB, mcRF;

	/**
	 * constructor for drivetrain initializes CANTalon stuff
	 */
	public Drivetrain() {
		mcLT = new CANTalon(6);
		mcLB = new CANTalon(10);
		mcLF = new CANTalon(11);
		mcRT = new CANTalon(9);
		mcRB = new CANTalon(8);
		mcRF = new CANTalon(5);

		mcLT.changeControlMode(TalonControlMode.Position);
		mcLB.changeControlMode(TalonControlMode.Follower);
		mcLF.changeControlMode(TalonControlMode.Follower);
		mcRT.changeControlMode(TalonControlMode.Position);
		mcRB.changeControlMode(TalonControlMode.Follower);
		mcRF.changeControlMode(TalonControlMode.Follower);

		mcLT.setFeedbackDevice(FeedbackDevice.QuadEncoder);
		mcRT.setFeedbackDevice(FeedbackDevice.QuadEncoder);

		mcLT.setPID(1.0, 0, 0);
		mcRT.setPID(1.0, 0, 0);

		mcLB.set(1);
		mcLF.set(1);
		mcRB.set(4);
		mcRF.set(4);
	}

	public void initDefaultCommand() {
		setDefaultCommand(new TeleopDriveyWivey());
	}

	public void initTeleop() {
		mcLT.changeControlMode(TalonControlMode.Speed);
		mcRT.changeControlMode(TalonControlMode.Speed);
	}

	public void initAutonomous() {
		mcLT.changeControlMode(TalonControlMode.Position);
		mcRT.changeControlMode(TalonControlMode.Position);
	}

	public void joystickDrive(Joystick stick) {
		mcLT.set(stick.getY() - stick.getX());
		mcRT.set(stick.getY() + stick.getX());

	}

	public void joystickFOCDrive(Joystick stick) {
		Robot.getRobotAngle();
		double PugAngle = Robot.getRobotAngle();
		double JoystickAngle = stick.getDirectionDegrees();

		if (JoystickAngle < PugAngle) {
			mcLT.set(1);
			mcRT.set(-1);
		} else if (JoystickAngle > PugAngle) {
			mcLT.set(-1);
			mcRT.set(1);
		} else {
			mcLT.set(stick.getMagnitude());
			mcRT.set(stick.getMagnitude());
		}

	}
	public void autoDrive(double x, double y) {
		double initangle = Math.atan(x / y); // Angle to the final position
		double initdistance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)); 
		// Distance
		// directly
		// to
	    // the
		// final
	    // position

		autoTurn(initangle);

		mcLT.set(initdistance);
		mcRT.set(initdistance);
	}
	
	/***
	 * autoTurn -- turn to a given angle change in degrees.
	 * @param hdg heading change in degrees. Positive values correspond to clockwise rotation.
	 */
	final double maxTurnOutput = 100.0;
	public void autoTurn(double hdg) {
		double startAngle = Robot.getRobotAngle();
		double endAngle = startAngle + hdg;
		
		mcLT.changeControlMode(TalonControlMode.Speed);
		mcRT.changeControlMode(TalonControlMode.Speed);
		
		while(true) {
			double curErr = endAngle - Robot.getRobotAngle();
			
			if(Math.abs(curErr) < 1) {
				break;
			}
			
			curErr = Math.min(curErr, 100);
			
			double out = (curErr / 100) * maxTurnOutput;
			
			if(hdg > 0) {
				mcLT.set(out);
				mcRT.set(-out);
			} else {
				mcLT.set(-out);
				mcRT.set(out);
			}
		}
		
		mcLT.changeControlMode(TalonControlMode.Position);
		mcRT.changeControlMode(TalonControlMode.Position);
	}

	public boolean isInPosition() {
		return mcLT.getClosedLoopError() + mcRT.getClosedLoopError() < 50;
	}

	public void stop() {
		mcLT.set(0);
		mcRT.set(0);
	}

	public boolean isSafe() {

		if (mcLT.getTemperature() < 200 && mcRT.getTemperature() < 200) {
			return true;
		}

		else {
			return false;
		}

	}

	public void updateSD() {
		SmartDashboard.putNumber("mc1 get", mcLT.get());
		SmartDashboard.putNumber("mc2 get", mcLB.get());
		SmartDashboard.putNumber("mc3 get", mcLF.get());
		SmartDashboard.putNumber("mc4 get", mcRT.get());
		SmartDashboard.putNumber("mc5 get", mcRB.get());
		SmartDashboard.putNumber("mc6 get", mcRF.get());
		SmartDashboard.putNumber("mc1 BusVoltage", mcLT.getBusVoltage());
		SmartDashboard.putNumber("mc2 BusVoltage", mcLB.getBusVoltage());
		SmartDashboard.putNumber("mc3 BusVoltage", mcLF.getBusVoltage());
		SmartDashboard.putNumber("mc4 BusVoltage", mcRT.getBusVoltage());
		SmartDashboard.putNumber("mc5 BusVoltage", mcRB.getBusVoltage());
		SmartDashboard.putNumber("mc6 BusVoltage", mcRF.getBusVoltage());
		SmartDashboard.putNumber("mc1 ClosedLoopError", mcLT.getClosedLoopError());
		SmartDashboard.putNumber("mc2 ClosedLoopError", mcLB.getClosedLoopError());
		SmartDashboard.putNumber("mc3 ClosedLoopError", mcLF.getClosedLoopError());
		SmartDashboard.putNumber("mc4 ClosedLoopError", mcRT.getClosedLoopError());
		SmartDashboard.putNumber("mc5 ClosedLoopError", mcRB.getClosedLoopError());
		SmartDashboard.putNumber("mc6 ClosedLoopError", mcRF.getClosedLoopError());
		SmartDashboard.putNumber("mc1 OutputVoltage", mcLT.getOutputVoltage());
		SmartDashboard.putNumber("mc2 OutputVoltage", mcLB.getOutputVoltage());
		SmartDashboard.putNumber("mc3 OutputVoltage", mcLF.getOutputVoltage());
		SmartDashboard.putNumber("mc4 OutputVoltage", mcRT.getOutputVoltage());
		SmartDashboard.putNumber("mc5 OutputVoltage", mcRB.getOutputVoltage());
		SmartDashboard.putNumber("mc6 OutputVoltage", mcRF.getOutputVoltage());
		SmartDashboard.putNumber("mc1 OutputCurrent", mcLT.getOutputCurrent());
		SmartDashboard.putNumber("mc2 OutputCurrent", mcLB.getOutputCurrent());
		SmartDashboard.putNumber("mc3 OutputCurrent", mcLF.getOutputCurrent());
		SmartDashboard.putNumber("mc4 OutputCurrent", mcRT.getOutputCurrent());
		SmartDashboard.putNumber("mc5 OutputCurrent", mcRB.getOutputCurrent());
		SmartDashboard.putNumber("mc6 OutputCurrent", mcRF.getOutputCurrent());

	}
}
