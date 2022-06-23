package com.cmdi.influxdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DevopsInfluxdbApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevopsInfluxdbApplication.class, args);
	}

	@Bean
	public ObjectMapper getObjectMapper(){
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper;
	}

}
