package trajectory;

public class GravityDragAcceleration extends AccelerateFunction {
	static final double air_density = 1.225; // kg / m^3
	static final double gravity = 9.81; // m / s^2
	
	private final double cross_section;
	private final double mass;
	private final double drag_coefficient;
	
	// drag_force_coefficient = 0.5*air_density*boulder_drag_coeff*cross_section; 
	
	
	double drag_force_coefficient;
	
	@Override
	void accelerate(BoulderState prevState, BoulderDeriviation outDeriv, double time, double delta) {
		double fDrag_x = drag_force_coefficient*(prevState.velX * prevState.velX);
		double fDrag_y = drag_force_coefficient*(prevState.velY * prevState.velY);
		
		outDeriv.accX = new Double(-1.0d * (fDrag_x / mass));
		outDeriv.accY = new Double(-1.0d * ((fDrag_y / mass) + gravity));
	}
	
	/*
	 * diameter is in meters
	 * Cd = drag coefficient (dimensionless)
	 * m = mass (kg)
	 */
	public GravityDragAcceleration(double diameter, double Cd, double m) {
		// cross sectional area = pi * r**2 (or equivalently (1/4)*pi*d**2)
		cross_section = (Math.PI * (diameter*diameter)) / 4;
		drag_coefficient = Cd;
		mass = m;
		
		drag_force_coefficient = (air_density*drag_coefficient*cross_section) / 2;
	}

}
