package org.usfirst.frc.team5002.robot.subsystems;

import org.usfirst.frc.team5002.robot.Robot;

import edu.wpi.first.wpilibj.command.Subsystem;

/**
 *
 */
public class Positioner extends Subsystem {
    
	class KalmanFilter {
	    /* Kalman Filter parameters: */
	    private double R;
	    private double Q;
	    
	    private double A;
	    private double B;
	    private double H;
	    
	    /* Previous outputs: */
	    private double prevOut;
	    private double prevErrCov;
	    
	    public double doFilter(double controlInput, double measurement) {
	        /* Time Update step: */
	        prevOut = (A*prevOut) + (B*controlInput);
	        prevErrCov = (A*A*prevErrCov) + Q;
	        
	        double gain = (prevErrCov*H) / (H*H*prevErrCov + R);
	        
	        /* Measurement Update step: */
	        prevOut = prevOut + (gain*(measurement - (H*prevOut)));
	        prevErrCov = (1 - (gain*H))*prevErrCov;
	        
	        return prevOut;
	    }
	    
	    public KalmanFilter(double a, double b, double h, double r, double q) {
	        this.R = r; // sensor covariance
	        this.Q = q; // world or model covariance
	        
	        this.A = a; // world state transition model matrix / coeff
	        this.B = b; // control input model matrix / coeff
	        this.H = h; // observation transformation matrix / coeff
	        
	        prevOut = 0;
	        prevErrCov = 0;
	    }
	}
	
	private double curX = 0;
	private double curY = 0;

	final KalmanFilter accelXFilter = new KalmanFilter(1, 1, 1, 0.000150, 1);
	final KalmanFilter accelYFilter = new KalmanFilter(1, 1, 1, 0.000150, 1);
	
	private int lastLVel = 0;
	private int lastRVel = 0;
	private long lastTS = 0;
	
	private double lastVelX = 0;
	private double lastVelY = 0;
	
	private long lastOdoTS = 0;
	
	// Accelerometer covariance: 150 micro g per square root hz (+- 0.000150)
	// Vision covariance: 2 inches per measurement
	// Odometer covariance: (wheel diameter / gear ratio * encoder resolution) inches per measurement
	
	/*
	final double wheelDiameter = 10.0;
	final double gearRatio;
	final double encoderResolution;
	
	final KalmanFilter odoFilter = new KalmanFilter(1, 1, 1, (wheelDiameter) / (gearRatio*encoderResolution), 1);
	*/
	
	public Positioner() {
		lastTS = System.nanoTime();
		lastOdoTS = System.nanoTime();
	}
	
    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        //setDefaultCommand(new MySpecialCommand());
    }
    
    public void setPosition(double x, double y) {
    	curX = x;
    	curY = y;
    }
    
    public void updatePosition(double x, double y) {
    	curX += x;
    	curY += y;
    }
    
    public void updateFromOdometry(double speed) {
    	long curTS = System.nanoTime();
    	long elapsed = curTS - lastOdoTS;
    	
    	lastOdoTS = curTS;
    	
    	double worldX = speed * Math.sin(Math.toRadians(Robot.ahrs.getYaw()));
    	double worldY = speed * Math.cos(Math.toRadians(Robot.ahrs.getYaw()));
    	
    	double elapsedSec = (elapsed / 1000000000);
    	
    	updatePosition(elapsedSec * worldX, elapsedSec * worldY);
    
    }
    
    public void updateFromAccelerometer() {
    	long curTS = System.nanoTime();
    	long elapsed = curTS - lastTS;
    	
    	double dVL = (Robot.drivetrain.getLVel() - lastLVel) / elapsed;
    	double dVR = (Robot.drivetrain.getRVel() - lastRVel) / elapsed;
    	
    	double dV = (dVL + dVR) / 2;
    	
    	double accX = Robot.ahrs.getWorldLinearAccelX(); // right or left
    	double accY = Robot.ahrs.getWorldLinearAccelY(); // forward or back
    	
    	double filteredX = accelXFilter.doFilter(dV, accX);
    	double filteredY = accelXFilter.doFilter(dV, accX);
    	
    	double rAccTheta = Math.atan2(filteredY, filteredX);
    	double rAccMag = Math.sqrt( (filteredX * filteredX) + (filteredY * filteredY) );
    	
    	double worldX = rAccMag * Math.sin(rAccTheta + Math.toRadians(Robot.ahrs.getYaw()));
    	double worldY = rAccMag * Math.cos(rAccTheta + Math.toRadians(Robot.ahrs.getYaw()));
    	
    	double elapsedSec = (elapsed / 1000000000);
    	
    	lastVelX += elapsedSec * worldX;
    	lastVelY += elapsedSec * worldY;
    	
    	updatePosition(elapsedSec * lastVelX, elapsedSec * lastVelY);
    }
}

