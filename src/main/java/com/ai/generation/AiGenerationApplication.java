package com.ai.generation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class AiGenerationApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiGenerationApplication.class, args);
	}

}
