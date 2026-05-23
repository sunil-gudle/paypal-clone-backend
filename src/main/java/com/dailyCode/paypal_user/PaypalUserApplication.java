package com.dailyCode.paypal_user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PaypalUserApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaypalUserApplication.class, args);
	}

}
