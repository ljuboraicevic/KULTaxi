package taxi;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;

public class GradientField {

	final RandomGenerator rng;
	final private RoadModel roadModel;
	final double signalDrop;
	final double taxiVSCustomer;
	
	ArrayList<Point> customerPositions;
	
	public final HashMap<String, ArrayList<Point>> graph = new HashMap<>();
	public final HashMap<Integer, Point> nodes = new HashMap<>();
	public final HashMap<Point, Integer> reverseNodes = new HashMap<>();
	
	public HashMap<Customer, Boolean> customersInTransport;
	
	public GradientField(RoadModel roadModel, RandomGenerator rng, double signalDrop, double taxiVSCustomer) {
		this.roadModel = roadModel;
		this.rng = rng;
		this.signalDrop = signalDrop;
		this.taxiVSCustomer = taxiVSCustomer;
		customerPositions = new ArrayList<>();
		customersInTransport = new HashMap<>();
	}
	
	public GradientFieldPoint getApproximateDirection(TaxiGradient vehicle) {
		ArrayList<Point> samples = samplePoints(vehicle);
		return getStrongestPoint(samples, roadModel.getPosition(vehicle));
	}
	
	private ArrayList<Point> samplePoints(TaxiGradient vehicle) {
		//take adjacent nodes from the graph from the last node that has been
		//visited (see javadoc for lastNode)
		ArrayList<Point> initial = new ArrayList<>(graph.get(vehicle.lastNode.toString()));
		return initial;
	}
	
	private GradientFieldPoint getStrongestPoint(ArrayList<Point> samples, Point a) {
		double max = Double.MIN_VALUE;
		Point maxPoint = samples.get(0);
		
		//System.out.println("At " + reverseNodes.get(a) +", considering: ");
		
		for (Point p: samples) {
			double strenght = calculateFieldStrengthAtPoint(p);
			//System.out.println(reverseNodes.get(p) + ": " + strenght);
			if (strenght > max) {
				max = strenght;
				maxPoint = p;
			}
		}
		//System.out.println("Chosen " + reverseNodes.get(maxPoint));
		return new GradientFieldPoint(maxPoint, max);
	}
	
	private double calculateFieldStrengthAtPoint(Point p) {
		double sum = 0;
		
		for (Point cp: customerPositions) {
			double dist = Point.distance(cp, p);
			sum += 1 / Math.pow(dist, signalDrop);
		}
		
		ArrayList<TaxiGradient> allTaxis = calculateTaxiPositions();
		
		for (TaxiGradient tg: allTaxis) {
			double dist = Point.distance(roadModel.getPosition(tg), p);
			sum -= (1 / Math.pow(dist, signalDrop)) * taxiVSCustomer;
		}
		
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
	
	private ArrayList<TaxiGradient> calculateTaxiPositions() {
		ArrayList<TaxiGradient> taxiPositions = new ArrayList<>();
		ArrayList<TaxiGradient> allTaxis = 
				new ArrayList<>(roadModel.getObjectsOfType(TaxiGradient.class));
		
		for (TaxiGradient t: allTaxis) {
			//find closest taxibase
			Point position = roadModel.getPosition(t);
			Point taxiBasePosition = roadModel.getPosition(RoadModels.findClosestObject(position, roadModel, TaxiBase.class));
			boolean atTheBase = position.equals(taxiBasePosition);
			
			//if taxi isn't transporting somebody and if it's not at the base
			if (!t.isDrivingACustomer() && !atTheBase) {
				taxiPositions.add(t);
			}
		}
		
		return taxiPositions;
	}
	
	public void loadGraphNew(String MAP_FILE, int lastNode) throws IOException {
		  Path path = Paths.get(MAP_FILE + "apos");
		  List<String> lines = Files.readAllLines(path, Charset.forName("ISO-8859-1"));
		  
		  Pattern pattern1 = Pattern.compile("\\'(.*?)\\,");
		  Pattern pattern2 = Pattern.compile("\\,(.*?)\\'");
		  
		  String x = "";
		  String y = "";
		  
		  for (int iCount = 1; iCount < lastNode + 2; iCount++) {
			  Matcher matcher1 = pattern1.matcher(lines.get(iCount));
			  while (matcher1.find()) {
				  x = matcher1.group(0);
				  x = x.substring(1, x.length()-1);
			  }
			  
			  Matcher matcher2 = pattern2.matcher(lines.get(iCount));
			  while (matcher2.find()) {
				  y = matcher2.group(0);
				  y = y.substring(1, y.length()-1);
			  }
			  
			  Point p = new Point(Double.parseDouble(x), Double.parseDouble(y));
			  nodes.put(iCount-1, p);
			  reverseNodes.put(p, iCount-1);
			  
		  }
		  
		  Pattern pattern3 = Pattern.compile("n(\\d*?)\\s");
		  Pattern pattern4 = Pattern.compile("\\sn(\\d*?)\\[");
		  
		  String z = "";
		  String a = "";
		  
		  for (int iCount = lastNode + 2; iCount < lines.size(); iCount++) {
			  
			  Matcher matcher3 = pattern3.matcher(lines.get(iCount));
			  while (matcher3.find()) {
				  z = matcher3.group(0);
				  z = z.substring(1, z.length()-1);
			  }
			  
			  Matcher matcher4 = pattern4.matcher(lines.get(iCount));
			  while (matcher4.find()) {
				  a = matcher4.group(0);
				  a = a.substring(2, a.length()-1);
			  }
			  
			  Point key = nodes.get(Integer.parseInt(z));
			  String keyStr = key.toString();
			  Point value = nodes.get(Integer.parseInt(a));
			  
			  if (graph.containsKey(keyStr)) {
				  ArrayList<Point> temp = graph.get(keyStr);
				  temp.add(value);
				  graph.put(keyStr, temp);
			  } else {
				  ArrayList<Point> temp = new ArrayList<Point>();
				  temp.add(value);
				  graph.put(keyStr, temp);
			  }
		  }
	  }
}