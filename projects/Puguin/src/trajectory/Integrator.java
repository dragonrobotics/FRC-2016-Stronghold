package trajectory;

public class Integrator {

	AccelerateFunction accFunc;
	
	public BoulderDeriviation derive(BoulderState prevState, BoulderDeriviation prevDeriv, double time, double delta) {
		BoulderState tmpState = new BoulderState();
		
		tmpState.posX = prevState.posX + (prevDeriv.velX * delta);
		tmpState.posY = prevState.posY + (prevDeriv.velY * delta);
		
		tmpState.velX = prevState.velX + (prevDeriv.accX * delta);
		tmpState.velY = prevState.velY + (prevDeriv.accY * delta);
		
		BoulderDeriviation newDeriv = new BoulderDeriviation();
		
		newDeriv.velX = prevState.velX;
		newDeriv.velY = prevState.velY;
		
		this.accFunc.accelerate(tmpState, newDeriv, time, delta);
		
		return newDeriv;
	}
	
	public void integrate(BoulderState state, double time, double delta) {
		BoulderDeriviation a = this.derive(state, new BoulderDeriviation(), time, 0.0);
		BoulderDeriviation b = this.derive(state, a, time, delta/2);
		BoulderDeriviation c = this.derive(state, b, time, delta/2);
		BoulderDeriviation d = this.derive(state, c, time, delta);
		
		double d_posX_dt = ( a.velX.doubleValue() + (2.0*(b.velX.doubleValue() + c.velX.doubleValue())) + d.velX.doubleValue() ) / 6.0;
		double d_posY_dt = ( a.velY.doubleValue() + (2.0*(b.velY.doubleValue() + c.velY.doubleValue())) + d.velY.doubleValue() ) / 6.0;
		
		double d_velX_dt = ( a.accX.doubleValue() + (2.0*(b.accX.doubleValue() + c.accX.doubleValue())) + d.accX.doubleValue() ) / 6.0;
		double d_velY_dt = ( a.accY.doubleValue() + (2.0*(b.accY.doubleValue() + c.accY.doubleValue())) + d.accY.doubleValue() ) / 6.0;
		
		state.posX = Double.sum(state.posX, new Double(d_posX_dt * delta));
		state.posY = new Double(state.posY.doubleValue() + (d_posY_dt * delta));
		
		state.velX = new Double(state.velX.doubleValue() + (d_velX_dt * delta));
		state.velY = new Double(state.velY.doubleValue() + (d_velY_dt * delta));
	}
	
	public Integrator(AccelerateFunction f) {
		this.accFunc = f;
	}

}
