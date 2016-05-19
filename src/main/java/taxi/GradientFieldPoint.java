package taxi;

import com.github.rinde.rinsim.geom.Point;

public final class GradientFieldPoint {
	public final Point point;
	public final double strength;
	
	public GradientFieldPoint(Point point, double strength) {
		this.point = point;
		this.strength = strength;
	}
}
