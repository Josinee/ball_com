package com.ballcom.ordering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ballcom.ordering", "com.ballcom.shared"})
public class OrderingApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderingApplication.class, args);
    }
}//TODO reageert nog niet op shipment dingen, staat als het delivered is op order status PLACED wat niet klopt, er is een orderStatus shipped en delivered
