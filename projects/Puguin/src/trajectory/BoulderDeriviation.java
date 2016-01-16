package trajectory;

/***
 * Container for derivative values for the integrator.
 * @author Sebastian Mobo
 *
 */
public class BoulderDeriviation {

	Double velX;
	Double velY;
	Double accX;
	Double accY;
	
	/***
	 * Create a new, empty BoulderDeriviation.
	 */
	public BoulderDeriviation() {
		this.velX = new Double(0.0);
		this.velY = new Double(0.0);
		this.accX = new Double(0.0);
		this.accY = new Double(0.0);
	}

}
