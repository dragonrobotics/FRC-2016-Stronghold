package trajectory;

public class TrajectoryPoint {

	Double time;
	Double posX;
	Double posY;
	
	public TrajectoryPoint(Double T, Double X, Double Y) {
		time = new Double(T);
		posX = new Double(X);
		posY = new Double(Y);
	}
	
	public TrajectoryPoint(double T, BoulderState State) {
		time = new Double(T);
		posX = new Double(State.posX);
		posY = new Double(State.posY);
	}

	public TrajectoryPoint(TrajectoryPoint copy) {
		time = new Double(copy.time);
		posX = new Double(copy.posX);
		posY = new Double(copy.posY);
	}

}
