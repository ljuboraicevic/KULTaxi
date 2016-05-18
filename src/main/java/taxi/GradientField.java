package taxi;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

public class GradientField {

	final RandomGenerator rng;
	final private RoadModel roadModel;
	

	public GradientField(RoadModel roadModel, RandomGenerator rng) {
		this.roadModel = roadModel;
		this.rng = rng;
	}
	
	public Point getApproximateDirection() {
		return roadModel.getRandomPosition(rng);
	}
}
