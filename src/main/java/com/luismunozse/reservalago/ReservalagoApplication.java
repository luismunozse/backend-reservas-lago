package com.luismunozse.reservalago;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReservalagoApplication {

	public static void main(String[] args) {

        SpringApplication.run(ReservalagoApplication.class, args);
	}

}
