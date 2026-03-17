package com.finger.handoff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class HandoffApplication {

	public static void main(String[] args) {
		SpringApplication.run(HandoffApplication.class, args);
	}

}