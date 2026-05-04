package com.fooddrinks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FoodsDrinksApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodsDrinksApplication.class, args);
    }
}
