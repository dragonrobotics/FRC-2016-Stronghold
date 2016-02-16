package org.usfirst.frc.team5002.robot.subsystems;

import org.usfirst.frc.team5002.robot.Robot;
import org.usfirst.frc.team5002.robot.commands.TeleopDriveyWivey;

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
	private CANTalon mc1, mc2, mc3, mc4, mc5, mc6;

	/**
	 * constructor for drivetrain initializes CANTalon stuff
	 */
	public Drivetrain() {
		mc1 = new CANTalon(31);
		mc2 = new CANTalon(32);
		mc3 = new CANTalon(33);
		mc4 = new CANTalon(34);
		mc5 = new CANTalon(35);
		mc6 = new CANTalon(36);

		mc1.changeControlMode(TalonControlMode.Position);		
		mc2.changeControlMode(TalonControlMode.Follower);
		mc3.changeControlMode(TalonControlMode.Follower);
		mc4.changeControlMode(TalonControlMode.Position);		
		mc5.changeControlMode(TalonControlMode.Follower);
		mc6.changeControlMode(TalonControlMode.Follower);

		mc1.setFeedbackDevice(FeedbackDevice.QuadEncoder);
		mc4.setFeedbackDevice(FeedbackDevice.QuadEncoder);
		
		mc1.setPID(1.0, 0, 0);
		mc4.setPID(1.0, 0, 0);
		
		mc2.set(1);
		mc3.set(1);
		mc5.set(4);
		mc6.set(4);
	}

	public void initDefaultCommand() {
		 setDefaultCommand(new TeleopDriveyWivey());
	}
	
	public void initTeleop() {
		mc1.changeControlMode(TalonControlMode.Speed);
		mc4.changeControlMode(TalonControlMode.Speed);
	}
	
	public void initAutonomous() {
		mc1.changeControlMode(TalonControlMode.Position);
		mc4.changeControlMode(TalonControlMode.Position);
	}

	public void joystickDrive(Joystick stick) {
		mc1.set(stick.getY() - stick.getX());
		mc4.set(stick.getY() + stick.getX());
		 
	} 

	public void joystickFOCDrive(Joystick stick) {
		Robot.getRobotAngle();
		double PugAngle = Robot.getRobotAngle();
		double JoystickAngle = stick.getDirectionDegrees();
		
		if (JoystickAngle < PugAngle){
			mc1.set(1);
			mc4.set(-1);
		}
		else if(JoystickAngle > PugAngle){
			mc1.set(-1);
			mc4.set(1);
		}
		else{
			mc1.set(stick.getMagnitude());
			mc4.set(stick.getMagnitude());
		}

	}

	public void autoDrive(double x, double y, double angle) {

	}

	public boolean isInPosition() {

		return false;
	}

	public void stop() {
		mc1.set(0);
		mc4.set(0);
	}

	public boolean isSafe() {

		if (mc1.getTemperature() < 200 && mc4.getTemperature() < 200) {
			return true;
		}

		else {
			return false;
		}

	}
	public void updateSD(){
		SmartDashboard.putNumber("mc1 get", mc1.get());
		SmartDashboard.putNumber("mc2 get", mc2.get());
		SmartDashboard.putNumber("mc3 get", mc3.get());
		SmartDashboard.putNumber("mc4 get", mc4.get());
		SmartDashboard.putNumber("mc5 get", mc5.get());
		SmartDashboard.putNumber("mc6 get", mc6.get());
		SmartDashboard.putNumber("mc1 BusVoltage", mc1.getBusVoltage());
		SmartDashboard.putNumber("mc2 BusVoltage", mc2.getBusVoltage());
		SmartDashboard.putNumber("mc3 BusVoltage", mc3.getBusVoltage());
		SmartDashboard.putNumber("mc4 BusVoltage", mc4.getBusVoltage());
		SmartDashboard.putNumber("mc5 BusVoltage", mc5.getBusVoltage());
		SmartDashboard.putNumber("mc6 BusVoltage", mc6.getBusVoltage());
		SmartDashboard.putNumber("mc1 ClosedLoopError", mc1.getClosedLoopError());
		SmartDashboard.putNumber("mc2 ClosedLoopError", mc2.getClosedLoopError());
		SmartDashboard.putNumber("mc3 ClosedLoopError", mc3.getClosedLoopError());
		SmartDashboard.putNumber("mc4 ClosedLoopError", mc4.getClosedLoopError());
		SmartDashboard.putNumber("mc5 ClosedLoopError", mc5.getClosedLoopError());
		SmartDashboard.putNumber("mc6 ClosedLoopError", mc6.getClosedLoopError());
		SmartDashboard.putNumber("mc1 OutputVoltage", mc1.getOutputVoltage());
		SmartDashboard.putNumber("mc2 OutputVoltage", mc2.getOutputVoltage());
		SmartDashboard.putNumber("mc3 OutputVoltage", mc3.getOutputVoltage());
		SmartDashboard.putNumber("mc4 OutputVoltage", mc4.getOutputVoltage());
		SmartDashboard.putNumber("mc5 OutputVoltage", mc5.getOutputVoltage());
		SmartDashboard.putNumber("mc6 OutputVoltage", mc6.getOutputVoltage());
		SmartDashboard.putNumber("mc1 OutputCurrent", mc1.getOutputCurrent());
		SmartDashboard.putNumber("mc2 OutputCurrent", mc2.getOutputCurrent());
		SmartDashboard.putNumber("mc3 OutputCurrent", mc3.getOutputCurrent());
		SmartDashboard.putNumber("mc4 OutputCurrent", mc4.getOutputCurrent());
		SmartDashboard.putNumber("mc5 OutputCurrent", mc5.getOutputCurrent());
		SmartDashboard.putNumber("mc6 OutputCurrent", mc6.getOutputCurrent());
		
		}
}
