package com.ballcom.ordering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ballcom.ordering", "com.ballcom.shared", "com.ballcom.customers"})
public class OrderingApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderingApplication.class, args);
    }
}