package trajectory;

import java.util.Vector;

public class Trajectory {

	enum GoalStatus {
		TRAJ_GOAL,			/* Trajectory reaches goal */
		TRAJ_TOO_SHORT,		/* Trajectory falls short of goal */
		TRAJ_TOO_HIGH,		/* Trajectory reaches goal distance too high (overshoot) */
		TRAJ_TOO_LOW		/* Trajectory reaches goal distance too low (undershoot) */
	};
	
	Vector<TrajectoryPoint> timeline;
	
	public Trajectory() {
		timeline = new Vector<TrajectoryPoint>();
	}
	
	public void addPoint(double time, BoulderState state) {
		timeline.add( new TrajectoryPoint(time, state) );
	}
	
	/* Get the point closest to a specified distance. */
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
	public Double getGoalErr(Double goalDistance) {
		TrajectoryPoint goalPoint = getClosestToDistance( goalDistance );
		return new Double(Math.abs( goalPoint.posY.doubleValue() - 2.4638 )); /* 2.4638 = height to goal + half height of goal */
	}
	
	public Double flightTime() {
		return new Double( timeline.lastElement().time );
	}
	
	public Double distance() {
		return new Double( timeline.lastElement().posX );
	}
	
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
