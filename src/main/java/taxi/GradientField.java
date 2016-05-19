package taxi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;

public class GradientField {

	final RandomGenerator rng;
	final private RoadModel roadModel;
	//final int taxiRepulsivness;
	//final double taxiRepRadius;
	final int custWaitingAmplification;
	final double signalDrop;
	final double taxiBaseVSCustomer;
	final double taxiVSCustomer;
	
	ArrayList<Point> customerPositions;
	ArrayList<Point> taxiBasePositions;
	
	HashMap<String, ArrayList<Point>> graph;
	HashMap<Point, Integer> reverseNodes;
	public HashMap<Customer, Boolean> customersInTransport;
	
	public GradientField(
			RoadModel roadModel, 
			RandomGenerator rng,
			//int taxiRepulsivness,
			//double taxiRepRadius,
			int custWaitingAmplification,
			double signalDrop,
			double taxiBaseVSCustomer,
			double taxiVSCustomer,
			HashMap<String, ArrayList<Point>> graph,
			HashMap<Point, Integer> reverseNodes) {
		
		this.roadModel = roadModel;
		this.rng = rng;
		//this.taxiRepulsivness = taxiRepulsivness;
		//this.taxiRepRadius = taxiRepRadius;
		this.custWaitingAmplification = custWaitingAmplification;
		this.signalDrop = signalDrop;
		this.taxiBaseVSCustomer = taxiBaseVSCustomer;
		this.taxiVSCustomer = taxiVSCustomer;
		customerPositions = new ArrayList<>();
		taxiBasePositions = new ArrayList<>();
		this.graph = graph;
		this.reverseNodes = reverseNodes;
		customersInTransport = new HashMap<>();
	}
	
	public GradientFieldPoint getApproximateDirection(TaxiGradient vehicle) {
		ArrayList<Point> samples = samplePoints(vehicle);
		return getStrongestPoint(samples, roadModel.getPosition(vehicle));
	}
	
	private ArrayList<Point> samplePoints(TaxiGradient vehicle) {
		//take adjacent nodes from the graph from the last node that has been
		//visited (since sometimes rinsim skips a tick or something, so we need
		//to keep track of which node was last visited and use that as our
		//approximate current position
		ArrayList<Point> initial = new ArrayList<>(graph.get(vehicle.lastNode.toString()));
		return initial;
	}
	
	private GradientFieldPoint getStrongestPoint(ArrayList<Point> samples, Point a) {
		double max = Double.MIN_VALUE;
		Point maxPoint = samples.get(0);
		
		System.out.println("At " + reverseNodes.get(a) +", considering: ");
		
		for (Point p: samples) {
			double strenght = calculateFieldStrengthAtPoint(p);
			System.out.println(reverseNodes.get(p) + ": " + strenght);
			if (strenght > max) {
				max = strenght;
				maxPoint = p;
			}
		}
		System.out.println("Chosen " + reverseNodes.get(maxPoint));
		return new GradientFieldPoint(maxPoint, max);
	}
	
	private double calculateFieldStrengthAtPoint(Point p) {
		double sum = 0;
		
		for (Point cp: customerPositions) {
			double dist = Point.distance(cp, p);
			sum += 1 / Math.pow(dist, signalDrop);
		}
		/*
		for (Point tbp: taxiBasePositions) {
			double dist = Point.distance(tbp, p);
			sum += (dist / Math.pow(dist, signalDrop)) * taxiBaseVSCustomer;
		}*/
		
		
		/*ArrayList<TaxiGradient> allTaxis = 
				new ArrayList<>(roadModel.getObjectsOfType(TaxiGradient.class));
		
		for (TaxiGradient tg: allTaxis) {
			double dist = Point.distance(roadModel.getPosition(tg), p);
			sum -= (dist / Math.pow(dist, signalDrop)) * taxiVSCustomer;
		}*/
		
		return sum;
	}
	
	public void calculateCustomerPositions() {
		customerPositions = new ArrayList<>();
		ArrayList<Customer> allCustomers = 
				new ArrayList<>(roadModel.getObjectsOfType(Customer.class));
		
		for (Customer c: allCustomers) {
			//get all the customers that aren't already in transport
			if (!customersInTransport.containsKey(c)) {
				customerPositions.add(roadModel.getPosition(c));
			}
		}
	}
	
	public void calculateTaxiBasePositions() {
		ArrayList<TaxiBase> allBases = 
				new ArrayList<>(roadModel.getObjectsOfType(TaxiBase.class));
		
		
		for (TaxiBase tb: allBases) {
			taxiBasePositions.add(roadModel.getPosition(tb));
		}
	}
}