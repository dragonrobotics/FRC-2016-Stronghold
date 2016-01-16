package trajectory;

import java.util.Vector;

/***
 * Holds trajectory data points from a given integration / simulation.
 * @author Sebastian Mobo
 *
 */
public class Trajectory {

	/***
	 * Returned status from checkForGoal().
	 * <li> TRAJ_GOAL: Trajectory reaches goal
	 * <li> TRAJ_TOO_SHORT: Trajectory falls short of goal horizontally
	 * <li> TRAJ_TOO_HIGH: Trajectory passes over goal
	 * <li> TRAJ_TOO_LOW: Trajectory falls under goal
	 * @author Sebastian Mobo
	 *
	 */
	enum GoalStatus {
		TRAJ_GOAL,			/* Trajectory reaches goal */
		TRAJ_TOO_SHORT,		/* Trajectory falls short of goal */
		TRAJ_TOO_HIGH,		/* Trajectory reaches goal distance too high (overshoot) */
		TRAJ_TOO_LOW		/* Trajectory reaches goal distance too low (undershoot) */
	};
	
	Vector<TrajectoryPoint> timeline;
	
	/***
	 * Construct a Trajectory with no data points.
	 */
	public Trajectory() {
		timeline = new Vector<TrajectoryPoint>();
	}
	
	/*** 
	 * Adds a point to this Trajectory.
	 * @param time Time of state
	 * @param state State of object in trajectory
	 */
	public void addPoint(double time, BoulderState state) {
		timeline.add( new TrajectoryPoint(time, state) );
	}
	
	/* Get the point closest to a specified distance. */
	/***
	 * Find the closest point to a given horizontal distance
	 * @param distance Distance to find
	 * @return Trajectory point closest to the given horizontal distance.
	 */
	public TrajectoryPoint getClosestToDistance(Double distance) {
		double dist = Math.abs( distance - timeline.firstElement().posX.doubleValue() );
		TrajectoryPoint closest = timeline.firstElement();
		for( TrajectoryPoint pt : timeline ) {
			if( Math.abs(pt.posX.doubleValue() - distance) < dist ) {
				closest = pt;
				dist = Math.abs(pt.posX.doubleValue() - distance);
			}
		}
		
		return closest;
	}
	
	/***
	 * Test to see if the Trajectory would pass through a FRC Stronghold high goal at a certain distance.
	 * @param distance Distance to high goal
	 * @return Goal status value.
	 */
	public GoalStatus checkForGoal(Double distance) {
		if(timeline.lastElement().posX < distance) {
			return GoalStatus.TRAJ_TOO_SHORT;
		}
		
		TrajectoryPoint closest = getClosestToDistance(distance);
		
		/* Is it reasonably close vertically? */
		if( closest.posY.doubleValue() >= (2.159+(.0254*2)) ) { /* 2.159 + (.0254*2) = 7ft 1in. + 2in. */
			if( closest.posY.doubleValue() <= (2.159+ (0.6096 - (.0254*2))) ) { /* 2.159+ (0.6096 - (.0254*2)) = 7ft 1in + (2ft - 2in) */
				return GoalStatus.TRAJ_GOAL;
			} else {
				return GoalStatus.TRAJ_TOO_HIGH;
			}
		} else {
			return GoalStatus.TRAJ_TOO_LOW;
		}
	}
	
	/* Get error from goal target. */
	/***
	 * Get error / distance from goal target.
	 * @param goalDistance Distance to FRC goal
	 * @return Distance from FRC goal.
	 */
	public Double getGoalErr(Double goalDistance) {
		TrajectoryPoint goalPoint = getClosestToDistance( goalDistance );
		return new Double(Math.abs( goalPoint.posY.doubleValue() - 2.4638 )); /* 2.4638 = height to goal + half height of goal */
	}
	
	/***
	 * Get total flight time of projectile.
	 * @return Projectile flight time.
	 */
	public Double flightTime() {
		return new Double( timeline.lastElement().time );
	}
	
	/***
	 * Get total horizontal distance traveled by projectile.
	 * @return Distance covered by projectile.
	 */
	public Double distance() {
		return new Double( timeline.lastElement().posX );
	}
	
	/***
	 * Get maximum height reached by projectile.
	 * @return Highest point reached by projectile.
	 */
	public TrajectoryPoint maxHeight() {
		TrajectoryPoint max = timeline.firstElement();
		for( TrajectoryPoint pt : timeline ) {
			if( pt.posY.doubleValue() > max.posY.doubleValue() ) {
				max = pt;
			}
		}
		
		return new TrajectoryPoint(max);
	}
	
	
}
