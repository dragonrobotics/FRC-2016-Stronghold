package trajectory;

/***
 * Base class for functions that model acceleration.
 * Not used directly: passed to Simulator and then to Integrator.
 * 
 * @author Tatantyler
 * 
 */
public abstract class AccelerateFunction {
	/***
	 * Calculate acceleration for a given object's state.
	 * @param prevState Object state to calculate acceleration for
	 * @param outDeriv Output object for acceleration vector values
	 * @param time Time to calculate acceleration from
	 * @param delta Time between integration steps
	 */
	abstract void accelerate(BoulderState prevState, BoulderDeriviation outDeriv, double time, double delta);
}