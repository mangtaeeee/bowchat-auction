package com.example.bowchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BowchatApplication {

	public static void main(String[] args) {
		SpringApplication.run(BowchatApplication.class, args);
	}

}
