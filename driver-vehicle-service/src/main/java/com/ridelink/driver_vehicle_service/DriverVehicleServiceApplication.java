package com.ridelink.driver_vehicle_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class DriverVehicleServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DriverVehicleServiceApplication.class, args);
	}

}
