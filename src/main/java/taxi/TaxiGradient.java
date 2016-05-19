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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * Implementation of a very simple taxi agent. It moves to the closest customer,
 * picks it up, then delivers it, repeat.
 *
 * @author Rinde van Lon
 */
class TaxiGradient extends Vehicle {
  private static final double SPEED = 5000d;
  private Optional<Parcel> curr;
  HashMap<String, ArrayList<Point>> graph;
  HashMap<Point, Integer> reverseNodes;
  /**
   * Different taxis can have different gas tank sizes.
   */
  private int tankSize;
  /**
   * Amount of gas (time) left.
   */
  private int gas;
  private GradientField field;
  private long skip;
  private ArrayList<Point> approximateDirection;

  TaxiGradient(Point startPosition, int capacity, int tankSize, int gas, GradientField field, HashMap<String, ArrayList<Point>> graph, HashMap<Point, Integer> reverseNodes) {
    super(VehicleDTO.builder()
      .capacity(capacity)
      .startPosition(startPosition)
      .speed(SPEED)
      .build());
    curr = Optional.absent();
    this.tankSize = tankSize;
    this.gas = gas;
    this.field = field;
    this.skip = 0;
    this.approximateDirection = null;
    this.graph = graph;
    this.reverseNodes = reverseNodes;
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  protected void tickImpl(TimeLapse time) {
    final RoadModel rm = getRoadModel();
    final PDPModel pm = getPDPModel();

    if (!time.hasTimeLeft()) { return; }

    //position at this tick
    Point position = rm.getPosition(this);
    
    final boolean inCargo;
    
    // is the taxi taken
    if (curr.isPresent()) { 
    	inCargo = pm.containerContains(this, curr.get());
    } else {
    	inCargo = false;
    }
    
    // if the taxi isn't driving a customer
    if (!inCargo) {
    	
    	//if the taxi is low on gas, go to the nearest gas station
    	/*if (lowGas()) {
    		GasStation closestGasStation = (GasStation) 
    				RoadModels.findClosestObject(position, rm, GasStation.class);
    		
    		rm.moveTo(this, closestGasStation, time);
    		//refill the tank when gas station is reached
    		if (rm.equalPosition(this, closestGasStation)) {
    			gas = tankSize;
    			System.out.println("refilled");
    		}
    	}*/
    	
    	//if the taxi is in one of the taxibases, don't consume gas
    	/*else if (position.equals(rm.getPosition(RoadModels.findClosestObject(position, rm, TaxiBase.class)))) {
    		//if taxi is at the taxi base -> add one, to counter balance the 
    		//gas-- at the end of the method; this is NOT refilling, just
    		//keeping the amount of gas the same
    		gas++;
    	}*/
    	
    	//at this point gas isn't low and taxi isn't in a taxi base
    	//else 
    	{
    		//FOLLOW THE GRADIENT FIELD
    		if (graph.containsKey(position.toString())) {
		    	approximateDirection = field.getApproximateDirection(this);
		    	String strPos;
    			if (reverseNodes.containsKey(position)) {
    				strPos = reverseNodes.get(position).toString();
    			} else {
    				strPos = position.toString();
    			}
    			System.out.println("Moving from " + strPos + " to " + reverseNodes.get(approximateDirection.get(0)));
    			System.out.println(" ");
    		}
    		/*System.out.println("From: " + position + " to " + approximateDirection);
    		System.out.println("      " + rm.getPosition(this));
    		System.out.println("      ");*/
    		
    		/*if (approximateDirection.x == 3293444.322625064) {
    			System.out.println("");
    		}*/
    		
    		//boolean success = false;
    		
    		//while (!success) {
	    	//	try {
	    			//System.out.println("Move to " + rm.getPosition(this) + " from " + approximateDirection.get(0));
	    			MoveProgress mp = rm.moveTo(this, approximateDirection.get(0), time);
	    			
	    			
	    			
	    			//String distance = mp.distance().toString();
	    			//System.out.println(distance);
	    			//if (!distance.equals("0.0 km")) {
	    	//			success = true;
	    			/*} else {
	    				success = false;
	    				System.out.println("Deleted " + approximateDirection.get(0));
		    			System.out.println("From " + position + " to " + approximateDirection.get(0));
		    			System.out.println(" ");
		    			approximateDirection.remove(0);
	    			}*/
	    	/*	} catch (Exception e) {
	    			System.out.println("-----");
	    			System.out.println("SKIPPED " + reverseNodes.get(approximateDirection.get(0)));
	    			System.out.println("From " + reverseNodes.get(position) + " to " + reverseNodes.get(approximateDirection.get(0)));
	    			System.out.println("Rerouting to " + reverseNodes.get(approximateDirection.get(1)));
	    			System.out.println("-----");
	    			
	    			//Random rand = new Random();
	    			//int  n = rand.nextInt(approximateDirection.size() - 1) + 1;
	    			//Point reroute = approximateDirection.get(n);
	    			//MoveProgress mp = rm.moveTo(this, reroute, time);
	    			
	    			approximateDirection.remove(0);
	    		}
    		}*/
    		
	        
	        //check if the taxi has reached a customer
	        ArrayList<Parcel> potentialCusts = 
	        		new ArrayList<>((HashSet<Parcel>) rm.getObjectsAt(this, Parcel.class));
	        
	        if (!potentialCusts.isEmpty()) {
	          // pickup customer
	          pm.pickup(this, potentialCusts.get(0), time);
	          curr = Optional.fromNullable(potentialCusts.get(0));
	          field.customersInTransport.put((Customer)curr.get(), true);
	          field.calculateCustomerPositions();
	          System.out.println("Customer picked up at " + reverseNodes.get(position) + " : " + position.toString());
	        }
    	}
    }
    // if there is a customer currently in the taxi
    else {
    	//go to its destination using the shortest path (not fields)
    	rm.moveTo(this, curr.get().getDeliveryLocation(), time);
    	// if we're at the destination
    	if (position.equals(curr.get().getDeliveryLocation())) {
    		// drop off passengers
    		System.out.println("Customer delivered at " + reverseNodes.get(curr.get().getDeliveryLocation()));
    		pm.deliver(this, curr.get(), time);
    		field.customersInTransport.remove((Customer)curr.get());
    		field.calculateCustomerPositions();
    		curr = Optional.absent();
    		
    	}
    }
    
    /* OLD CODE
    
    // if the taxi isn't assigned to a customer go to the nearest taxi base
    if (!curr.isPresent()) {
    	
    	//if the taxi is low on gas, go to the nearest gas station
    	if (lowGas()) {
    		GasStation closestGasStation = (GasStation) 
    				RoadModels.findClosestObject(position, rm, GasStation.class);
    		
    		rm.moveTo(this, closestGasStation, time);
    		//refill the tank when gas station is reached
    		if (rm.equalPosition(this, closestGasStation)) {
    			gas = tankSize;
    			System.out.println("refilled");
    		}
    	}
    	//this part is replaced by taxibases' field
    	
    	// if gas isn't low
    	//else {
	    	//TaxiBase closestBase = (TaxiBase) RoadModels.findClosestObject(position, rm, TaxiBase.class);
	    	//if (!position.equals(rm.getPosition(closestBase))) {
	    		//rm.moveTo(this, closestBase, time);
	    	//} else {
	    		//if taxi is at the taxi base -> add one, to counter balance the 
	    		//gas-- at the end of the method; this is NOT refilling, just
	    		//keeping the amount of gas the same
	    		//gas++;
	    	//}
    	//}
    }
    // if it is assigned
    if (curr.isPresent()) {
      final boolean inCargo = pm.containerContains(this, curr.get());
      // sanity check: if it is not in our cargo AND it is also not on the
      // RoadModel, we cannot go to curr anymore.
      if (!inCargo && !rm.containsObject(curr.get())) {
        curr = Optional.absent();
      } else if (inCargo) {
        // if it is in cargo, go to its destination
        rm.moveTo(this, curr.get().getDeliveryLocation(), time);
        // if we're at the destination
        if (position.equals(curr.get().getDeliveryLocation())) {
          // drop off passengers
          pm.deliver(this, curr.get(), time);
          curr = Optional.absent();
        }
      }
      // if there is no customer in the taxi
      else {
    	//FOLLOW THE GRADIENT FIELD
    	Point approximateDirection = field.getApproximateDirection();
        rm.moveTo(this, approximateDirection, time);
        
        //check if there's a customer at current location
        ArrayList<Customer> potentialCusts = 
        		new ArrayList<>((HashSet<Customer>) rm.getObjectsAt(this, Customer.class));
        
        if (!potentialCusts.isEmpty()) {
          // pickup customer
          pm.pickup(this, potentialCusts.get(0), time);
        }
      }
    }
    
     */
    
    //reduce amount of gas
    gas--;
    skip++;
  }
  
  /**
   * Checks if gas level is below 20%.
   * @return
   */
  private boolean lowGas() {
	  return gas < Math.round(tankSize / 5);
  }
}
