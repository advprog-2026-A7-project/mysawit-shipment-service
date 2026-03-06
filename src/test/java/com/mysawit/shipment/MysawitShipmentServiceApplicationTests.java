package com.mysawit.shipment;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.*;

class MysawitShipmentServiceApplicationTests {

    @Test
    void constructorCanBeCalled() {
        new MysawitShipmentServiceApplication();
    }

    @Test
    void mainRunsSpringApplication() {
        String[] args = new String[] {"--spring.main.web-application-type=none"};
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            MysawitShipmentServiceApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(MysawitShipmentServiceApplication.class, args));
        }
    }
}
