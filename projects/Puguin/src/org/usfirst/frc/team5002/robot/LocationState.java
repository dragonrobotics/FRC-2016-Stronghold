package org.usfirst.frc.team5002.robot;

import java.util.Vector;

public class LocationState {
	private double x, y;
	private double heading;
	
	Vector<Double> x_estimates;
	Vector<Double> y_estimates;
	Vector<Double> heading_estimates;
	
	public LocationState(double x, double y) {
		this.x = x;
		this.y = y;
		this.heading = 0;
		
		this.x_estimates = new Vector<Double>();
		this.y_estimates = new Vector<Double>();
		this.heading_estimates = new Vector<Double>();
	}
	
	public LocationState() {
		this.x = 0;
		this.y = 0;
		this.heading = 0;
		
		this.x_estimates = new Vector<Double>();
		this.y_estimates = new Vector<Double>();
		this.heading_estimates = new Vector<Double>();
	}

	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getHeading() {
		return heading;
	}
	
	public void addPositionUpdate(double x, double y) {
		x_estimates.addElement(new Double(x));
		y_estimates.addElement(new Double(y));
	}
	
	public void addHeadingUpdate(double hdg) {
		heading_estimates.addElement(new Double(hdg));
	}
	
	public void update() {
		double xUpdate = 0;
		double yUpdate = 0;
		double hdgUpdate = 0;
		
		for(Double i : x_estimates) {
			xUpdate += i.doubleValue();
		}
		
		xUpdate /= x_estimates.size();
		
		for(Double i : y_estimates) {
			yUpdate += i.doubleValue();
		}
		
		yUpdate /= y_estimates.size();
		
		for(Double i : heading_estimates) {
			hdgUpdate += i.doubleValue();
		}
		
		hdgUpdate /= heading_estimates.size();
		
		this.x += xUpdate;
		this.y += yUpdate;
		this.heading += hdgUpdate;
		
		x_estimates.clear();
		y_estimates.clear();
		heading_estimates.clear();
	}
}
