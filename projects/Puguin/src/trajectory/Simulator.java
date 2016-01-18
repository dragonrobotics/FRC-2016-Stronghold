package trajectory;

/***
 * Calculates ballistic trajectories using the Integrator.
 * @author Sebastian Mobo
 *
 */
public class Simulator {
	/***
	 * Time between steps in the integration.
	 * Smaller timesteps are more precise but take more work to calculate.
	 */
	static final double timestep = 0.05;
	final double mass;
	
	Integrator integrator;
	
	/***
	 * Simulate a projectile launch.
	 * @param launchAngle Angle to launch at, in degrees.
	 * @param launchPower Force to launch with, in newtons.
	 * @param launchElevation Initial elevation of projectile.
	 * @return Trajectory data points from the launch.
	 */
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
	
	/***
	 * Attempts to find a launch angle given a launch power, elevation, and the distance to a 2016 FRC goal.
	 * Returned values should at least be near the goal, but may not actually reach the goal if given extremely low power settings.
	 * @param goalDistance Distance from goal (high tower, FRC Stronghold 2016)
	 * @param launchPower Force of launch
	 * @param launchElevation Initial height of projectile
	 * @return Best launch angle found and trajectory
	 */
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
	
	/***
	 * Constructs a new Simulator.
	 * @param accF Acceleration function, passed to Integrator constructor
	 * @param m Mass of projectiles to simulate.
	 */
	public Simulator(AccelerateFunction accF, double m) {
		integrator = new Integrator(accF);
		mass = m;
	}

}
