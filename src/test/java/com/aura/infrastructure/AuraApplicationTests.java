package com.aura.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuraApplicationTests {

    @Test
    void contextLoads() {
        // Verify that the Spring application context loads without errors
    }
}
