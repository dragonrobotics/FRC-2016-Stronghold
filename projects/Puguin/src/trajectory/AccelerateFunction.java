package trajectory;

public abstract class AccelerateFunction {
	abstract void accelerate(BoulderState prevState, BoulderDeriviation outDeriv, double time, double delta);
}