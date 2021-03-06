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
class Taxi extends Vehicle implements TaxiInterface {
  private static final double SPEED = 10000d;
  private Optional<Parcel> curr;
  private long distance;
  private int customersServed;
  private SimpleLogger log;
  /**
   * Different taxis can have different gas tank sizes.
   */
  private int tankSize;
  /**
   * Amount of gas (time) left.
   */
  private int gas;

  Taxi(Point startPosition, int capacity, int tankSize, int gas, SimpleLogger log) {
    super(VehicleDTO.builder()
      .capacity(capacity)
      .startPosition(startPosition)
      .speed(SPEED)
      .build());
    curr = Optional.absent();
    this.tankSize = tankSize;
    this.gas = gas;
    this.distance = 0; 
    this.customersServed = 0;
    this.log = log;
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

  @Override
  protected void tickImpl(TimeLapse time) {
    final RoadModel rm = getRoadModel();
    final PDPModel pm = getPDPModel();

    if (!time.hasTimeLeft()) {
      return;
    }

    Point position = rm.getPosition(this);
    
    // if the taxi isn't assigned to a customer go to the nearest taxi base
    if (!curr.isPresent()) {
    	
    	//if the taxi is low on gas, go to the nearest gas station
    	if (lowGas()) {
    		GasStation closestGasStation = (GasStation) RoadModels.findClosestObject(position, rm, GasStation.class);
    		rm.moveTo(this, closestGasStation, time);
    		//refill the tank when gas station is reached
    		if (rm.equalPosition(this, closestGasStation)) {
    			gas = tankSize;
    			System.out.println("refilled");
    		}
    	}
    	// if gas isn't low
    	else {
	    	TaxiBase closestBase = (TaxiBase) RoadModels.findClosestObject(position, rm, TaxiBase.class);
	    	if (!position.equals(rm.getPosition(closestBase))) {
	    		rm.moveTo(this, closestBase, time);
	    	} else {
	    		//if taxi is at the taxi base -> add one, to counter balance the 
	    		//gas-- at the end of the method; this is NOT refilling, just
	    		//keeping the amount of gas the same
	    		gas++;
	    		distance--;
	    	}
    	}
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
          log.logCustomerDelivered(curr.get(), time.getTime());
          curr = Optional.absent();
          customersServed++;
        }
      } else {
        // it is still available, go there as fast as possible
        rm.moveTo(this, curr.get(), time);
        if (rm.equalPosition(this, curr.get())) {
          // pickup customer
          pm.pickup(this, curr.get(), time);
          log.logCustomerPickedUp(curr.get(), time.getTime());
        }
      }
    }
    
    //reduce amount of gas
    gas--;
    distance++;
  }
  
  public void assignCustomer(Parcel customer) {
	  curr = Optional.fromNullable(customer);
  }
  
  public boolean isFree() {
	  return !curr.isPresent() && !lowGas();
  }

  /**
   * Checks if gas level is below 20%.
   * @return
   */
  private boolean lowGas() {
	  return gas < Math.round(tankSize / 5);
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
