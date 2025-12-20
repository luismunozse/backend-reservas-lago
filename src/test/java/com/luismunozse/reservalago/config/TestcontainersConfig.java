package com.luismunozse.reservalago.config;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.TimeZone;

public class TestcontainersConfig {

    public static final PostgreSQLContainer<?> postgres;

    static {
        // Forzar UTC en la JVM para que el driver JDBC no envíe la timezone del sistema
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");

        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                .withDatabaseName("lago_test")
                .withUsername("test")
                .withPassword("test")
                .withEnv("TZ", "UTC")
                .withCommand("-c", "timezone=UTC");
        postgres.start();
    }
}
