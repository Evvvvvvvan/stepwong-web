package com.example.stepwong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StepwongWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(StepwongWebApplication.class, args);
    }
}
