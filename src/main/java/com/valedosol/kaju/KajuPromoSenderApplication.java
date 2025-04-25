package com.valedosol.kaju;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KajuPromoSenderApplication {
	public static void main(String[] args) {
		SpringApplication.run(KajuPromoSenderApplication.class, args);
	}

}
