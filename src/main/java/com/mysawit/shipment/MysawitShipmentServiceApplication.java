package com.mysawit.shipment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class MysawitShipmentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MysawitShipmentServiceApplication.class, args);
    }
}
