package taxi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

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
	
	public double getMeanTimeFromRegisterToPickup() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Parcel c: customers) {
			try {
			stats.addValue(customerPickupTime.get(c) - customerRegistrationTime.get(c));
			} catch (Exception e) {}
		}
		return stats.getMean();
	}
	
	public double getSDTimeFromRegisterToPickup() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Parcel c: customers) {
			try {
			stats.addValue(customerPickupTime.get(c) - customerRegistrationTime.get(c));
			} catch (Exception e) {}
		}
		return stats.getStandardDeviation();
	}
	
	public double getMeanTimeFromRegisterToDelivery() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Parcel c: customers) {
			try {
			stats.addValue(customerDeliveryTime.get(c) - customerRegistrationTime.get(c));
			} catch (Exception e) {}
		}
		return stats.getMean();
	}
	
	public double getSDTimeFromRegisterToDelivery() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Parcel c: customers) {
			try {
			stats.addValue(customerDeliveryTime.get(c) - customerRegistrationTime.get(c));
			} catch (Exception e) {}
		}
		return stats.getStandardDeviation();
	}
	
	public double getMeanDistanceCoveredByTaxis() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (TaxiInterface t: taxis) {
			try {
			stats.addValue(t.getDistanceCovered());
			} catch (Exception e) {}
		}
		return stats.getMean();
	}
	
	public double getSDDistanceCoveredByTaxis() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (TaxiInterface t: taxis) {
			try {
			stats.addValue(t.getDistanceCovered());
			} catch (Exception e) {}
		}
		return stats.getStandardDeviation();
	}
	
	public double getMeanNoOfCustomersServedByTaxis() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (TaxiInterface t: taxis) {
			try {
			stats.addValue(t.getNumberOfCustomersServed());
			} catch (Exception e) {}
		}
		return stats.getMean();
	}
	
	public double getSDNoOfCustomersServedByTaxis() {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (TaxiInterface t: taxis) {
			try {
			stats.addValue(t.getNumberOfCustomersServed());
			} catch (Exception e) {}
		}
		return stats.getStandardDeviation();
	}
	
	public void printAllStatistics() {
		System.out.println("customer 1) register time 2) pickup time 3) delivery time");
		printAllCustomerRawData();
        System.out.println("distance per taxi");
        printDistanceCoveredPerTaxi();
        System.out.println("customers served per taxi");
        printNumberOfCustomersServedPerTaxi();
        System.out.println("mean register to pickup time");
        System.out.println(getMeanTimeFromRegisterToPickup());
        System.out.println("sd register to pickup time");
        System.out.println(getSDTimeFromRegisterToPickup());
        System.out.println("mean register to delivery time");
        System.out.println(getMeanTimeFromRegisterToDelivery());
        System.out.println("sd register to delivery time");
        System.out.println(getSDTimeFromRegisterToDelivery());
        System.out.println("mean distance covered by taxis");
        System.out.println(getMeanDistanceCoveredByTaxis());
        System.out.println("sd distance covered by taxis");
        System.out.println(getSDDistanceCoveredByTaxis());
        System.out.println("mean customers served by taxis");
        System.out.println(getMeanNoOfCustomersServedByTaxis());
        System.out.println("sd customers served by taxis");
        System.out.println(getSDNoOfCustomersServedByTaxis());
	}
}
