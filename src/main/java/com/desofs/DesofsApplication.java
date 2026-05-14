package com.desofs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DesofsApplication {
    public static void main(String[] args) {
        SpringApplication.run(DesofsApplication.class, args);
    }
}
