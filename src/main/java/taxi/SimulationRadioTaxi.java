/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package taxi;

import static com.google.common.collect.Maps.newHashMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.MultiAttributeData;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.io.DotGraphIO;
import com.github.rinde.rinsim.geom.io.Filters;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;

/**
 * Example showing a fleet of taxis that have to pickup and transport customers
 * around the city of Leuven.
 * <p>
 * If this class is run on MacOS it might be necessary to use
 * -XstartOnFirstThread as a VM argument.
 * @author Rinde van Lon
 */
public final class SimulationRadioTaxi {

	private static final double RADIUS = 20000;
	private static final int MAX_TANK = 5000;
	private static final int NUM_GAS_STATIONS = 3;
	private static final long STANDARD_SIM_TIME = 8 * 60 * 60 * 1000;
	
	/******************/
	
  private static final int NUM_DEPOTS = 1;
  private static final int NUM_TAXIS = 2;
  /**
   * Initial number of customers
   */
  private static final int NUM_CUSTOMERS = 1;

  // time in ms
  private static final long SERVICE_DURATION = 60000;
  private static final int TAXI_CAPACITY = 10;
  private static final int DEPOT_CAPACITY = 100;

  private static final int SPEED_UP = 4;
  private static final int MAX_CAPACITY = 3;
  private static final double NEW_CUSTOMER_PROB = .001;

  private static final String MAP_FILE = "maps\\test.dot";
  private static final Map<String, Graph<MultiAttributeData>> GRAPH_CACHE =
    newHashMap();

  private static final long TEST_STOP_TIME = 20 * 60 * 1000;
  private static final int TEST_SPEED_UP = 64;

  private SimulationRadioTaxi() {}

  /**
   * Starts the {@link SimulationRadioTaxi}.
   * @param args The first option may optionally indicate the end time of the
   *          simulation.
   */
  public static void main(@Nullable String[] args) {
	  if (NUM_CUSTOMERS > NUM_TAXIS) { 
		  System.out.println("Number of initial customers is greater than the number of taxis");
		  System.exit(1);
	  }
    final long endTime = args != null && args.length >= 1 ? Long
      .parseLong(args[0]) : STANDARD_SIM_TIME;

    final String graphFile = args != null && args.length >= 2 ? args[1]
      : MAP_FILE;
    run(false, endTime, graphFile, null /* new Display() */, null, null);
  }

  /**
   * Run the example.
   * @param testing If <code>true</code> enables the test mode.
   */
  public static void run(boolean testing) {
    run(testing, Long.MAX_VALUE, MAP_FILE, null, null, null);
  }

  /**
   * Starts the example.
   * @param testing Indicates whether the method should run in testing mode.
   * @param endTime The time at which simulation should stop.
   * @param graphFile The graph that should be loaded.
   * @param display The display that should be used to show the ui on.
   * @param m The monitor that should be used to show the ui on.
   * @param list A listener that will receive callbacks from the ui.
   * @return The simulator instance.
   */
  public static Simulator run(boolean testing, final long endTime,
      String graphFile,
      @Nullable Display display, @Nullable Monitor m, @Nullable Listener list) {

    final View.Builder view = createGui(testing, display, m, list);

    // use map of leuven
    final Simulator simulator = Simulator.builder()
      .addModel(RoadModelBuilders.staticGraph(loadGraph(graphFile)))
      .addModel(DefaultPDPModel.builder())
      .addModel(view)
      .build();
    
    final RandomGenerator rng = simulator.getRandomGenerator();

    final RoadModel roadModel = simulator.getModelProvider().getModel(RoadModel.class);
    
    final SimpleLogger log = new SimpleLogger();
    
    //add depots
    for (int i = 0; i < NUM_DEPOTS; i++) {
      simulator.register(new TaxiBase(roadModel.getRandomPosition(rng), DEPOT_CAPACITY));
    }
    
	//add gas stations
    for (int i = 0; i < NUM_GAS_STATIONS; i++) {
      simulator.register(new GasStation(roadModel.getRandomPosition(rng), DEPOT_CAPACITY));
    }
    
    //this list is used to assign initial customers to taxis - doesn't take
    //the distance between the taxi and the customer into account; first customer
    //gets the first taxi etc
    ArrayList<Taxi> initialListOfTaxies = new ArrayList<>();
    // add taxis
    for (int i = 0; i < NUM_TAXIS; i++) {
    	int tankSize = (MAX_TANK / 2) + rng.nextInt(MAX_TANK / 2);
    	int gas = Math.round(tankSize / 3) + rng.nextInt(tankSize / 2);
    	Taxi taxi = new Taxi(roadModel.getRandomPosition(rng), TAXI_CAPACITY, tankSize, gas, log);
    	simulator.register(taxi);
    	initialListOfTaxies.add(taxi);
    	log.registerTaxi(taxi);
    }
    
    // add customers and assign them to taxis
    for (int i = 0; i < NUM_CUSTOMERS; i++) {
    	Customer cust = generateNewRandomCustomer(roadModel, rng);
    	simulator.register(cust);
    	//RoadModels.findClosestObject(cust.getPickupLocation(), roadModel, Taxi.class).assingCustomer(cust);
    	initialListOfTaxies.get(i).assignCustomer(cust);
    	log.logCustomerRegistered(cust, 0);
    }
    
    simulator.addTickListener(new TickListener() {
    	ArrayList<Parcel> bufferedCustomers = new ArrayList<>();
    	double radius = RADIUS;
    	
      @Override
      public void tick(TimeLapse time) {
    	//stop the simulation if time runs out
        if (time.getStartTime() > endTime) {
          simulator.stop();
          log.printAllStatistics();
        } 
        // if we still have time, roll the dice and maybe add a new customer
        else {
        	if (rng.nextDouble() < NEW_CUSTOMER_PROB) {
        		Parcel cust = generateNewRandomCustomer(roadModel, rng);
	        	bufferedCustomers.add(cust);
	        	simulator.register(cust);
	        	log.logCustomerRegistered(cust, time.getTime());
        	}
        	
        	//if there are some customers that haven't been assigned to a taxi
        	//assign them now
        	if (bufferedCustomers.size() > 0) {
        		Parcel cust = bufferedCustomers.get(0);
        		Taxi taxi = callForTaxi(cust.getPickupLocation(), roadModel, rng, radius);
	        	if (taxi != null) {
	        		taxi.assignCustomer(cust);
	        		bufferedCustomers.remove(0);
	        		radius = RADIUS;
	        	}
	        	radius += 0.5 * RADIUS;
        	}
        }
      }

      @Override
      public void afterTick(TimeLapse timeLapse) {}
    });
    
    
    simulator.start();

    return simulator;
  }
  
  /**
   * Try to find a free taxi within a given radius. If it fails, another call
   * will be made, with increased radius from the tick listener. 
   * 
   * @param custLocation Customers position
   * @param rm
   * @param rng
   * @return First taxi to respond to the broadcast (kind of)
   */
  private static Taxi callForTaxi(Point custLocation, RoadModel rm, RandomGenerator rng, double radius) {

	  Collection<Taxi> taxisWithinRadius = new ArrayList<>();
	  ArrayList<Taxi> freeTaxisWithinRadius = new ArrayList<>();
	  
	  taxisWithinRadius = RoadModels.findObjectsWithinRadius(
				  custLocation, 
				  rm, 
				  radius, 
				  Taxi.class);
		  
	  for (Taxi t: taxisWithinRadius) {
		  if (t.isFree()) {
			  freeTaxisWithinRadius.add(t);
		  }
	  }
		  
	  if (freeTaxisWithinRadius.isEmpty()) { 
		  return null; 
	  } else {
		  int chosenTaxi = rng.nextInt(freeTaxisWithinRadius.size());
		  return freeTaxisWithinRadius.get(chosenTaxi);
	  }
  }
  
  private static Customer generateNewRandomCustomer(RoadModel rm, RandomGenerator rng) {
	  Point custLocation = rm.getRandomPosition(rng);
	  Point custDestination = rm.getRandomPosition(rng);
  	
	  return new Customer(
              Parcel.builder(custLocation, custDestination)
          	.serviceDuration(SERVICE_DURATION)
          	.neededCapacity(1 + rng.nextInt(MAX_CAPACITY))
          	.buildDTO());
  }

  static View.Builder createGui(
      boolean testing,
      @Nullable Display display,
      @Nullable Monitor m,
      @Nullable Listener list) {

    View.Builder view = View.builder()
      .with(GraphRoadModelRenderer.builder())
      .with(RoadUserRenderer.builder()
        .withImageAssociation(
          TaxiBase.class, "/graphics/perspective/tall-building-64.png")
        .withImageAssociation(
          Taxi.class, "/graphics/flat/taxi-32.png")
        .withImageAssociation(
          Customer.class, "/graphics/flat/person-red-32.png")
        .withImageAssociation(
        	GasStation.class, 
        	"/graphics/flat/warehouse-32.png"))
      //.with(TaxiRenderer.builder(TaxiRenderer.Language.ENGLISH))
      .withTitleAppendix("Taxi Demo");

    if (testing) {
      view = view.withAutoClose()
        .withAutoPlay()
        .withSimulatorEndTime(TEST_STOP_TIME)
        .withSpeedUp(TEST_SPEED_UP);
    } else if (m != null && list != null && display != null) {
      view = view.withMonitor(m)
        .withSpeedUp(SPEED_UP)
        .withResolution(m.getClientArea().width, m.getClientArea().height)
        .withDisplay(display)
        .withCallback(list)
        .withAsync()
        .withAutoPlay()
        .withAutoClose();
    }
    return view;
  }

  // load the graph file
  static Graph<MultiAttributeData> loadGraph(String name) {
    try {
      if (GRAPH_CACHE.containsKey(name)) {
        return GRAPH_CACHE.get(name);
      }
      final Graph<MultiAttributeData> g = DotGraphIO
        .getMultiAttributeGraphIO(
          Filters.selfCycleFilter())
        .read(
          name);

      GRAPH_CACHE.put(name, g);
      return g;
    } catch (final FileNotFoundException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
