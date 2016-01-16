package trajectory;

/***
 * Stores data for points in a trajectory.
 * @author Sebastian Mobo
 *
 */
public class TrajectoryPoint {

	Double time;
	Double posX;
	Double posY;
	
	/***
	 * Create a new TrajectoryPoint from X/Y coordinates.
	 * @param T time at point
	 * @param X X position at point
	 * @param Y Y position at point
	 */
	public TrajectoryPoint(Double T, Double X, Double Y) {
		time = new Double(T);
		posX = new Double(X);
		posY = new Double(Y);
	}
	
	/***
	 * Create a new TrajectoryPoint from an object's state.
	 * @param T time at point
	 * @param State state of object at point
	 */
	public TrajectoryPoint(double T, BoulderState State) {
		time = new Double(T);
		posX = new Double(State.posX);
		posY = new Double(State.posY);
	}

	/***
	 * Copy a TrajectoryPoint.
	 * @param copy TrajectoryPoint to copy from.
	 */
	public TrajectoryPoint(TrajectoryPoint copy) {
		time = new Double(copy.time);
		posX = new Double(copy.posX);
		posY = new Double(copy.posY);
	}

}
