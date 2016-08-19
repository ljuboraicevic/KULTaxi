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
class TaxiGradient extends Vehicle implements TaxiInterface {
  private static final double SPEED = 5000d;
  private long distance;
  private int customersServed;
  private int taxiID;
  SimpleLogger log;
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
  /**
   * Gradient field, used for task allocation.
   */
  private GradientField field;
  /**
   * Ordered list of preferences for the next node-destination
   */
  private GradientFieldPoint approximateDirection;
  
  /**
   * Current position of the taxi
   */
  private Point currentPosition;
  
  /**
   * Sometimes rinsim skips a tick or something, so we need
   * to keep track of which node was last visited and use that as our
   * approximate current position
   */
  public Point lastNode;  

  TaxiGradient(
		  Point startPosition, 
		  int capacity, 
		  int tankSize, 
		  int gas, 
		  GradientField field, 
		  SimpleLogger log,
		  int taxiID) {
	  
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
    this.currentPosition = startPosition;
    this.lastNode = startPosition;
    this.distance = 0; 
    this.customersServed = 0;
    this.log = log;
    this.taxiID = taxiID;
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  protected void tickImpl(TimeLapse time) {
    final RoadModel rm = getRoadModel();
    final PDPModel pm = getPDPModel();

    if (!time.hasTimeLeft()) { return; }

    //position at this tick
    currentPosition = rm.getPosition(this);

    //if taxi is currently at one of the nodes
    if (field.graph.containsKey(currentPosition.toString())) {
    	lastNode = currentPosition;
    	//if it's at a node calculate approximate direction based on the field
    	approximateDirection = field.getApproximateDirection(this);
    	//printMovingFromTo();
    	//System.out.println(String.format("TAXI %d AT NODE %d", taxiID,field.reverseNodes.get(currentPosition)));
    }
    
    // if the taxi isn't driving a customer
    if (!isDrivingACustomer()) {
    	//if the taxi is low on gas, go to the nearest gas station
    	if (lowGas()) {
    		GasStation closestGasStation = (GasStation) 
    				RoadModels.findClosestObject(currentPosition, rm, GasStation.class);
    		
    		rm.moveTo(this, closestGasStation, time);
    		//refill the tank when gas station is reached
    		if (rm.equalPosition(this, closestGasStation)) {
    			gas = tankSize;
    			System.out.println(String.format("TAXI %d REFILLED", taxiID));
    		}
    	} 
    	
    	// if there are no customers, go to the nearest base
    	else if (approximateDirection.strength <= 0.000000000001) {
    		TaxiBase closestBase = (TaxiBase) RoadModels.findClosestObject(currentPosition, rm, TaxiBase.class);
	    	if (!currentPosition.equals(rm.getPosition(closestBase))) {
	    		rm.moveTo(this, closestBase, time);
	    	} else {
	    		//if taxi is at the taxi base -> add one, to counter balance the 
	    		//gas-- at the end of the method; this is NOT refilling, just
	    		//keeping the amount of gas the same
	    		gas++;
	    		distance--;
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
	        	Parcel customer = potentialCusts.get(0);
	        	pickUpCustomer(customer, pm, time);
	        	System.out.println(String.format("CUSTOMER PICKED UP AT %d BY TAXI %d",field.reverseNodes.get(customer.getPickupLocation()),taxiID));
	        }
    	}
    }
    // if there is a customer currently in the taxi
    else {
    	Point customerDestination = curr.get().getDeliveryLocation();
    	//go to its destination using the shortest path (not gradient field)
    	rm.moveTo(this, customerDestination, time);
    	// if we're at the destination
    	if (currentPosition.equals(customerDestination)) {
    		// deliver passengers

    		deliverCustomer(curr.get(), pm, time);
    		
    	}
    }
    
    //reduce amount of gas
    gas--;
    distance++;
  }
  
  /**
   * Checks if taxi is currently driving a customer
   * 
   * @return 
   */
  public boolean isDrivingACustomer() {
	  return curr.isPresent();
  }
  
  /**
   * Pick up customer without printing
   * 
   * @param c
   * @param pm
   * @param time
   */
  private void pickUpCustomer(Parcel c, PDPModel pm, TimeLapse time) {
	  pickUpCustomer(c, pm, time, true);
  }
  
  /**
   * Picks up the customer and does the bookkeeping necessary to maintain
   * the gradient field.
   * 
   * @param c Customer to be picked up
   * @param pm PDPModel
   * @param time Simulation TimeLapse
   * @param print true if a message is to be written to stdout, false otherwise
   */
  private void pickUpCustomer(Parcel c, PDPModel pm, TimeLapse time, boolean print) {
	  pm.pickup(this, c, time);

	  if (print) {
		  System.out.println("Customer picked up at " 
				  + field.reverseNodes.get(currentPosition) 
				  + " : " 
				  + currentPosition.toString());
	  }
  	
	  //THIS CODE IS VERY IMPORTANT FOR GRADIENT FIELD BOOKKEEPING
	  curr = Optional.fromNullable(c);
      field.customersInTransport.put((Customer)curr.get(), true);
      field.updateCustomerPositions();
      customersServed++;
      log.logCustomerPickedUp(c, time.getTime());
  }
  
  /**
   * Deliver customer without printing
   * 
   * @param c
   * @param pm
   * @param time
   */
  private void deliverCustomer(Parcel c, PDPModel pm, TimeLapse time) {
	  deliverCustomer(c, pm, time, true);
  }
  
  /**
   * Delivers the customer and does the bookkeeping necessary to maintain
   * the gradient field.
   * 
   * @param c Customer to be delivered
   * @param pm PDPModel
   * @param time Simulation TimeLapse
   * @param print true if a message is to be written to stdout, false otherwise
   */
  private void deliverCustomer(Parcel c, PDPModel pm, TimeLapse time, boolean print) {
	  if (print) {
		  System.out.println(String.format("CUSTOMER DELIVERED AT %d",
					field.reverseNodes.get(curr.get().getDeliveryLocation())));
	  }
	  
	  pm.deliver(this, curr.get(), time);
	  
	  //CODE BELOW IS VERY IMPORTANT FOR GRADIENT FIELD BOOKKEEPING
	  field.customersInTransport.remove((Customer)curr.get());
	  field.updateCustomerPositions();
	  log.logCustomerDelivered(curr.get(), time.getTime());
	  curr = Optional.absent();
  }
  
  /**
   * Checks if gas level is below 20%.
   * 
   * @return
   */
  private boolean lowGas() {
	  return gas < Math.round(tankSize / 5);
  }
  
  /**
   * Prints current location and the location the taxi is moving towards.
   */
  private void printMovingFromTo() {
	String strPos;
	if (field.reverseNodes.containsKey(currentPosition)) {
		strPos = field.reverseNodes.get(currentPosition).toString();
	} else {
		strPos = currentPosition.toString();
	}
	System.out.println("Moving from " 
				+ strPos 
				+ " to " 
				+ field.reverseNodes.get(approximateDirection.point));
	System.out.println(" ");
  }

	@Override
	public long getDistanceCovered() {
		return this.distance;
	}
	
	@Override
	public int getNumberOfCustomersServed() {
		return customersServed;
	}
}
