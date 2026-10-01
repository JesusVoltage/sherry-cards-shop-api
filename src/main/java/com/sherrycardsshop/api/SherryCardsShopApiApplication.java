package com.sherrycardsshop.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SherryCardsShopApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SherryCardsShopApiApplication.class, args);
    }
}