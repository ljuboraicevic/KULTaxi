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
import java.util.HashSet;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
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
  /**
   * Parcel that is currently being delivered.
   */
  private Optional<Parcel> curr;
  /**
   * Different taxis can have different gas tank sizes.
   */
  private int tankSize;
  /**
   * Amount of gas (time) left.
   */
  private int gas;
  private GradientField field;
  /**
   * Ordered list of preferences for the next node-destination
   */
  private GradientFieldPoint approximateDirection;
  /**
   * Sometimes rinsim skips a tick or something, so we need
   * to keep track of which node was last visited and use that as our
   * approximate current position
   */
  public Point lastNode;

  TaxiGradient(Point startPosition, int capacity, int tankSize, int gas, GradientField field) {
    super(VehicleDTO.builder()
      .capacity(capacity)
      .startPosition(startPosition)
      .speed(SPEED)
      .build());
    curr = Optional.absent();
    this.tankSize = tankSize;
    this.gas = gas;
    this.field = field;
    this.approximateDirection = null;
    this.lastNode = startPosition;
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

    //sometimes rinsim skips a tick or something, so we need
	//to keep track of which node was last visited and use that as our
	//approximate current position
    if (field.graph.containsKey(position.toString())) {
    	lastNode = position;
    	
    	approximateDirection = field.getApproximateDirection(this);
    	
    	//code below is used for printing
    	/*String strPos;
		if (reverseNodes.containsKey(position)) {
			strPos = reverseNodes.get(position).toString();
		} else {
			strPos = position.toString();
		}
		System.out.println("Moving from " 
				+ strPos 
				+ " to " 
				+ reverseNodes.get(approximateDirection.point));
		System.out.println(" ");*/
    }
    
    // if the taxi isn't driving a customer
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
    	
    	// if there are no customers, go to the nearest base
    	else if (approximateDirection.strength <= 0.000000000001) {
    		TaxiBase closestBase = (TaxiBase) RoadModels.findClosestObject(position, rm, TaxiBase.class);
	    	if (!position.equals(rm.getPosition(closestBase))) {
	    		rm.moveTo(this, closestBase, time);
	    	} else {
	    		//if taxi is at the taxi base -> add one, to counter balance the 
	    		//gas-- at the end of the method; this is NOT refilling, just
	    		//keeping the amount of gas the same
	    		gas++;
	    	}
    	}
    	
    	//at this point gas isn't low and taxi isn't IN or MOVING TO a taxi base
    	else 
    	{
    		//follow the gradient field
	    	rm.moveTo(this, approximateDirection.point, time);
	    			
	        //check if the taxi has reached a customer
	        ArrayList<Parcel> potentialCusts = 
	        		new ArrayList<>((HashSet<Parcel>) rm.getObjectsAt(this, Parcel.class));
	        
	        if (!potentialCusts.isEmpty()) {
	        	// pickup customer
	        	pm.pickup(this, potentialCusts.get(0), time);

	        	System.out.println("Customer picked up at " 
	            		+ field.reverseNodes.get(position) 
	            		+ " : " 
	            		+ position.toString());
	        	
	        	//THIS CODE IS VERY IMPORTAND FOR GRADIENT FIELD BOOKKEEPING, SHOULD
	    		//BE USED IN ANY PLACE WHERE CUSTOMER IS DELIVERED
	            curr = Optional.fromNullable(potentialCusts.get(0));
	            field.customersInTransport.put((Customer)curr.get(), true);
	            field.calculateCustomerPositions();
	        }
    	}
    }
    // if there is a customer currently in the taxi
    else {
    	//go to its destination using the shortest path (not gradient field)
    	rm.moveTo(this, curr.get().getDeliveryLocation(), time);
    	// if we're at the destination
    	if (position.equals(curr.get().getDeliveryLocation())) {
    		// drop off passengers
    		System.out.println("Customer delivered at " 
    				+ field.reverseNodes.get(curr.get().getDeliveryLocation()));
    		
    		pm.deliver(this, curr.get(), time);
    		//THIS CODE IS VERY IMPORTAND FOR GRADIENT FIELD BOOKKEEPING, SHOULD
    		//BE USED IN ANY PLACE WHERE CUSTOMER IS DELIVERED
    		field.customersInTransport.remove((Customer)curr.get());
    		field.calculateCustomerPositions();
    		curr = Optional.absent();
    	}
    }
    
    //reduce amount of gas
    gas--;
  }
  
  /**
   * Checks if gas level is below 20%.
   * @return
   */
  private boolean lowGas() {
	  return gas < Math.round(tankSize / 5);
  }
}
