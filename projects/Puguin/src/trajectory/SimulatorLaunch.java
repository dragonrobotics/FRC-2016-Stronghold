package trajectory;

/***
 * Stores a launch angle and its associated trajectory.
 * Holds values returned from findLaunchAngle().
 * @author Sebastian Mobo
 *
 */
public class SimulatorLaunch {
	Double launchAngle;
	Trajectory traj;
	
	/***
	 * Construct a new SimulatorLaunch pair.
	 * @param angle
	 * @param t
	 */
	public SimulatorLaunch(Double angle, Trajectory t) {
		launchAngle = angle;
		traj = t;
	}
}
