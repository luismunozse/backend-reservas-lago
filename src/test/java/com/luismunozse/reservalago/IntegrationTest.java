package com.luismunozse.reservalago;

import com.luismunozse.reservalago.config.TestcontainersConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = TestcontainersConfig.postgres.getJdbcUrl() + "&options=-c%20timezone=UTC";
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", TestcontainersConfig.postgres::getUsername);
        registry.add("spring.datasource.password", TestcontainersConfig.postgres::getPassword);
    }
}
