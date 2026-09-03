package com.nexus.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class CoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoreApplication.class, args);
	}

	@Bean
	ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@Bean
	ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	RestClient restClient() {
		return RestClient.create();
	}

}
