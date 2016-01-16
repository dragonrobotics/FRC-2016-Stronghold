package trajectory;

public class Simulator {
	static final double timestep = 0.05;
	final double mass;
	
	Integrator integrator;
	
	public Trajectory simulate(double launchAngle, double launchPower, double launchElevation) {
		BoulderState curState = new BoulderState();
		double curTime = 0.0;
		
		double vel0 = launchPower / mass;
		
		curState.posX = new Double(0);
		curState.posY = launchElevation;
		
		curState.velX = vel0 * Math.cos( launchAngle * (Math.PI / 180.0) );
		curState.velY = vel0 * Math.sin( launchAngle * (Math.PI / 180.0) );
		
		Trajectory traj = new Trajectory();
		
		while(curState.posY >= 0) {
			traj.addPoint(curTime, curState);
			
			integrator.integrate(curState, curTime, timestep);
			curTime += timestep;
		}
		
		return traj;
	}
	
	/* Attempt to find a suitable launch angle given launch power and elevation. 
	 * Returned values should at least be near the goal, but may not actually reach the goal. */
	public SimulatorLaunch findLaunchAngle(Double goalDistance, double launchPower, double launchElevation) {
		Trajectory bestGuess = null;
		double guessAngle = 0.0;
		Double lastErr = null;
		Double bestErr = null;
		
		for( double curAngle = 20.0; curAngle <= 80.0; curAngle += 0.5 ) {
			Trajectory curGuess = simulate(curAngle, launchPower, launchElevation);
			
			if( curGuess.distance().doubleValue() < goalDistance.doubleValue() ) {
				continue;
			}
			
			Double curErr = curGuess.getGoalErr(goalDistance);
			
			if( lastErr != null ) {
				if( lastErr.doubleValue() < curErr.doubleValue() ) {
					/* Error values are increasing now, we might as well just quit now since we're straying */
					break;
				}
			}
			
			lastErr = curErr;
			
			if( bestErr == null ) {
				bestGuess = curGuess;
				bestErr = curErr;
				guessAngle = curAngle;
				continue;
			}
			
			if( curErr.doubleValue() < bestErr.doubleValue() ) { 
				/* This guess is closer than the last one. */
				bestErr = curErr;
				bestGuess = curGuess;
				guessAngle = curAngle;
			}
		}
		
		return new SimulatorLaunch(guessAngle, bestGuess);
	}
	
	public Simulator(AccelerateFunction accF, double m) {
		integrator = new Integrator(accF);
		mass = m;
	}

}
