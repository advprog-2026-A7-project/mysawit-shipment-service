package com.mysawit.shipment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MysawitShipmentServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void mainRunsWithTestProfile() {
        MysawitShipmentServiceApplication.main(
            new String[] {
                "--spring.profiles.active=test",
                "--spring.main.web-application-type=none",
                "--app.test.close-context=true"
            }
        );
    }
}
