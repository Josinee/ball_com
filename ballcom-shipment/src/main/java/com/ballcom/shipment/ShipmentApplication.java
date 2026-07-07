package com.ballcom.shipment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ballcom.shared", "com.ballcom.shipment"})
public class ShipmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShipmentApplication.class, args);
    }
} //TODO heeft geen customer en geen adres daardoor
//weet ook niet zeker of de prijs nou klopt die betaald is