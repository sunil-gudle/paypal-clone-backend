package com.dailyCode.paypal_user;

import com.dailyCode.paypal_user.config.EmbeddedRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(EmbeddedRedisConfig.class)
class PaypalUserApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full Spring context loads successfully
        // using H2 (in-memory DB) and embedded Redis
    }
}
