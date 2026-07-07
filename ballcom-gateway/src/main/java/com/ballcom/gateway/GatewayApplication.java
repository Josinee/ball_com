package com.ballcom.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    //TODO alles, blijkbaar moeten events in json verstuurd worden, niet in een generic domain record omdat dan de microservices niet autonoom zijn
}