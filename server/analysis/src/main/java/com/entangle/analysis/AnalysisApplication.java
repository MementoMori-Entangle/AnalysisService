package com.entangle.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.entangle.analysis")
@EnableJpaRepositories(basePackages = "com.entangle.analysis.repository")
@EntityScan(basePackages = "com.entangle.analysis.entity")
public class AnalysisApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnalysisApplication.class, args);
	}

}
