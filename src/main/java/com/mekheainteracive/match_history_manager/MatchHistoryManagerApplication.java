package com.mekheainteracive.match_history_manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MatchHistoryManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MatchHistoryManagerApplication.class, args);
	}

}
