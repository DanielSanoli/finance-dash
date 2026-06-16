package com.sanoli.financedash.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RailwayDatabaseUrlEnvironmentPostProcessorTest {

    private final RailwayDatabaseUrlEnvironmentPostProcessor postProcessor = new RailwayDatabaseUrlEnvironmentPostProcessor();

    @Test
    void shouldConvertRailwayPostgresUrlToJdbcUrl() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "DATABASE_URL", "postgres://demo:secret@containers-us-west-1.railway.app:5432/railway"
        )));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://containers-us-west-1.railway.app:5432/railway");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("demo");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("secret");
    }

    @Test
    void shouldKeepJdbcUrlWhenAlreadyConfigured() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "DATABASE_URL", "jdbc:postgresql://localhost:5432/financedash"
        )));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://localhost:5432/financedash");
    }
}
