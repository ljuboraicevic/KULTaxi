package taxi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.rinde.rinsim.core.model.pdp.Parcel;

public class SimpleLogger {
	private List<Parcel> customers;
	private Map<Parcel, Long> customerRegistrationTime;
	private Map<Parcel, Long> customerPickupTime;
	private Map<Parcel, Long> customerDeliveryTime;
	private List<TaxiInterface> taxis;
	
	SimpleLogger() {
		this.customers = new ArrayList<>();
		this.customerRegistrationTime = new HashMap<>();
		this.customerPickupTime = new HashMap<>();
		this.customerDeliveryTime = new HashMap<>();
		this.taxis = new ArrayList<>();
	}
	
	public void logCustomerRegistered(Parcel customer, long time) {
		customers.add(customer);
		customerRegistrationTime.put(customer, time);
	}
	
	public void logCustomerPickedUp(Parcel customer, long time) {
		customerPickupTime.put(customer, time);
	}
	
	public void logCustomerDelivered(Parcel customer, long time) {
		customerDeliveryTime.put(customer, time);
	}
	
	public void printAllCustomerRawData() {
		for (Parcel customer: customers) {
			System.out.println(customerRegistrationTime.get(customer));
			System.out.println(customerPickupTime.get(customer));
			System.out.println(customerDeliveryTime.get(customer));
			System.out.println(" ");
		}
	}
	
	public void printDistanceCoveredPerTaxi() {
		for (TaxiInterface taxi: taxis) {
			System.out.println(taxi.getDistanceCovered());
		}
	}
	
	public void printNumberOfCustomersServedPerTaxi() {
		for (TaxiInterface taxi: taxis) {
			System.out.println(taxi.getNumberOfCustomersServed());
		}
	}
	
	public void registerTaxi(TaxiInterface taxi) {
		taxis.add(taxi);
	}
	
	public long getAverageTimeFromRegisterToPickup() {
		//TODO
		return 0;
	}
	
	public long getSDTimeFromRegisterToPickup() {
		//TODO
		return 0;
	}
	
	public long getAverageTimeFromRegisterToDelivery() {
		//TODO
		return 0;
	}
	
	public long getSDTimeFromRegisterToDelivery() {
		//TODO
		return 0;
	}
	
	public long getAverageDistanceCoveredByTaxis() {
		//TODO
		return 0;
	}
	
	public long getSDDistanceCoveredByTaxis() {
		//TODO
		return 0;
	}
	
	public long getAverageNoOfCustomersServedByTaxis() {
		//TODO
		return 0;
	}
	
	public long getSDNoOfCustomersServedByTaxis() {
		//TODO
		return 0;
	}
}
