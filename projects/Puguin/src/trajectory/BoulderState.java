package trajectory;

/***
 * Holds position and velocity for simulated / integrated objects.
 * @author Sebastian Mobo
 *
 */
public class BoulderState {

	Double posX;
	Double posY;
	Double velX;
	Double velY;
	
	/***
	 * Create a new, empty BoulderState.
	 */
	public BoulderState() {
		this.posX = new Double(0.0d);
		this.posY = new Double(0.0d);
		this.velX = new Double(0.0d);
		this.velY = new Double(0.0d);
	}

}
