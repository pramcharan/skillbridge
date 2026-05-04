package com.skillbridge;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableAsync
public class SkillBridgeApplication {

	public static void main(String[] args) {
		// Set timezone to UTC before any database connections are made
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(SkillBridgeApplication.class, args);
	}

}
