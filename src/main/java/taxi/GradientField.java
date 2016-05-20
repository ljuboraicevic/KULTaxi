package taxi;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;

public class GradientField {

	final RandomGenerator rng;
	final private RoadModel roadModel;
	
	/**
	 * Signal strength drops with 1/(distance^signalDrop)   
	 */
	final double signalDrop;
	
	/**
	 * Ratio of taxi repulsiveness versus customer attraction
	 */
	final double taxiVSCustomer;
	
	/**
	 * Current list of customer positions. It is updated with 
	 * calculateCustomerPositions method (this method must be called after every
	 * customer pickup and drop off)
	 */
	ArrayList<Point> customerPositions;
	
	/**
	 * Map. Is used since information about adjacent nodes on the map is needed.
	 */
	public final HashMap<String, ArrayList<Point>> graph = new HashMap<>();
	
	/**
	 * List of all nodes loaded from the file (their n# and position)
	 */
	public final HashMap<Integer, Point> nodes = new HashMap<>();
	
	/**
	 * Reverse of nodes. Position is key and n# is value
	 */
	public final HashMap<Point, Integer> reverseNodes = new HashMap<>();
	
	/**
	 * List of customers currently being transported. Is used to ignore
	 * customers that are being transported when calculating the gradient. Has
	 * to be updated on every customer pickup and drop off.
	 * 
	 * TODO This can probably be replaced with ParcelState
	 */
	public HashMap<Customer, Boolean> customersInTransport;
	
	public GradientField(RoadModel roadModel, RandomGenerator rng, double signalDrop, double taxiVSCustomer) {
		this.roadModel = roadModel;
		this.rng = rng;
		this.signalDrop = signalDrop;
		this.taxiVSCustomer = taxiVSCustomer;
		customerPositions = new ArrayList<>();
		customersInTransport = new HashMap<>();
	}
	
	/**
	 * Checks vehicle's local environment and returns the place with the 
	 * strongest gradient field.
	 * 
	 * @param vehicle
	 * @return Point with the strongest gradient and its strength
	 */
	public GradientFieldPoint getApproximateDirection(TaxiGradient vehicle) {
		ArrayList<Point> samples = samplePoints(vehicle);
		return getStrongestPoint(samples, vehicle);
	}
	
	/**
	 * Returns adjacent nodes in the graph from the last node that has been 
	 * visited (see lastNode's javadoc for for why lastNode and not 
	 * current location is being used)
	 * 
	 * @param vehicle
	 * @return
	 */
	private ArrayList<Point> samplePoints(TaxiGradient vehicle) {
		ArrayList<Point> result = new ArrayList<>(graph.get(vehicle.lastNode.toString()));
		return result;
	}
	
	/**
	 * Takes samples (adjacent nodes) and finds the one that has the strongest
	 * gradient field.
	 * 
	 * @param samples
	 * @param vehicle
	 * @return
	 */
	private GradientFieldPoint getStrongestPoint(ArrayList<Point> samples, RoadUser vehicle) {
		double max = Double.MIN_VALUE;
		Point maxPoint = samples.get(0);
		
		for (Point p: samples) {
			double strenght = calculateFieldStrengthAtPoint(p, vehicle);
			if (strenght > max) {
				max = strenght;
				maxPoint = p;
			}
		}
		
		return new GradientFieldPoint(maxPoint, max);
	}
	
	/**
	 * Calculates gradient field strength at a given point.
	 * This is done by taking distances from all pending customers, doing a 
	 * 1 / distance^signalDrop and summing it up. Additionally, the same thing
	 * is done with other active taxi distances, but this is subtracted from the
	 * sum.
	 * 
	 * @param p
	 * @param vehicle
	 * @return
	 */
	private double calculateFieldStrengthAtPoint(Point p, RoadUser vehicle) {
		double sum = 0;
		
		for (Point cp: customerPositions) {
			double dist = Point.distance(cp, p);
			sum += 1 / Math.pow(dist, signalDrop);
		}
		
		ArrayList<Point> taxiPositions = calculateTaxiPositions(vehicle);
		
		for (Point tp: taxiPositions) {
			double dist = Point.distance(tp, p);
			sum -= (1 / Math.pow(dist, signalDrop)) * taxiVSCustomer;
		}
		
		return sum;
	}
	
	/**
	 * Updates current locations of all customers that are waiting for service.
	 */
	public void updateCustomerPositions() {
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
	
	/**
	 * Calculates current locations of all active taxis.
	 * (Active meaning not at a TaxiBase or currently driving a customer
	 * 
	 * @param vehicle
	 * @return List of all active taxis
	 */
	private ArrayList<Point> calculateTaxiPositions(RoadUser vehicle) {
		ArrayList<Point> taxiPositions = new ArrayList<>();
		ArrayList<TaxiGradient> allTaxis = 
				new ArrayList<>(roadModel.getObjectsOfType(TaxiGradient.class));
		
		for (TaxiGradient t: allTaxis) {
			// don't add the taxi for which the field is being calculated
			if (t.equals((TaxiGradient)vehicle)) { continue; }
			//find closest TaxiBase
			Point position = roadModel.getPosition(t);
			Point taxiBasePosition = roadModel.getPosition(RoadModels.findClosestObject(position, roadModel, TaxiBase.class));
			boolean atTheBase = position.equals(taxiBasePosition);
			
			//if taxi isn't transporting somebody and if it's not at the base
			if (!t.isDrivingACustomer() && !atTheBase) {
				taxiPositions.add(roadModel.getPosition(t));
			}
		}
		
		return taxiPositions;
	}
	
	/**
	 * Loads a .dot file to a graph using some extremely unsophisticated regex.
	 * 
	 * TODO fix regex so that there is no need to convert " to ' and use a 
	 * separate file
	 * TODO automatically determine the lastNode
	 * 
	 * @param MAP_FILE Path to the file
	 * @param lastNode n# of the last node in the file
	 * @throws IOException
	 */
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