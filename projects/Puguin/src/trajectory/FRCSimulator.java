package trajectory;

public class FRCSimulator extends Simulator {
	static final double boulder_diameter = .254; // m
	static final double boulder_mass = .294835; // kg
	static final double boulder_drag_coeff = 0.47; // (dimensionless)
	
	public FRCSimulator() {
		super(new GravityDragAcceleration(boulder_diameter, boulder_drag_coeff, boulder_mass), boulder_mass);
	}
}
