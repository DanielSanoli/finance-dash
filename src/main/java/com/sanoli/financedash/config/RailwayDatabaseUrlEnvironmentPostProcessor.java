package com.sanoli.financedash.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RailwayDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "railwayDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> properties = new HashMap<>();

        applyRailwayPostgresParts(environment, properties);

        String databaseUrl = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("DATABASE_PRIVATE_URL"),
                environment.getProperty("POSTGRES_URL")
        );

        if (databaseUrl == null) {
            // PGHOST/PGPORT/PGDATABASE are enough to build the connection.
        } else if (databaseUrl.startsWith("jdbc:postgresql://") && !hasInvalidJdbcDatabaseName(databaseUrl)) {
            properties.put("spring.datasource.url", databaseUrl);
        } else if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            applyPostgresUrl(databaseUrl, environment, properties);
        }

        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    private void applyRailwayPostgresParts(ConfigurableEnvironment environment, Map<String, Object> properties) {
        String host = firstNonBlank(environment.getProperty("PGHOST"), environment.getProperty("POSTGRES_HOST"));
        String database = firstNonBlank(environment.getProperty("PGDATABASE"), environment.getProperty("POSTGRES_DB"));

        if (host == null || database == null) {
            return;
        }

        String port = firstNonBlank(environment.getProperty("PGPORT"), environment.getProperty("POSTGRES_PORT"));
        String jdbcUrl = "jdbc:postgresql://" + host + (port == null ? "" : ":" + port) + "/" + database;
        properties.put("spring.datasource.url", jdbcUrl);

        putIfAvailable(properties, "spring.datasource.username",
                firstNonBlank(environment.getProperty("PGUSER"), environment.getProperty("POSTGRES_USER")),
                environment.getProperty("DATABASE_USERNAME"),
                environment.getProperty("SPRING_DATASOURCE_USERNAME"));
        putIfAvailable(properties, "spring.datasource.password",
                firstNonBlank(environment.getProperty("PGPASSWORD"), environment.getProperty("POSTGRES_PASSWORD")),
                environment.getProperty("DATABASE_PASSWORD"),
                environment.getProperty("SPRING_DATASOURCE_PASSWORD"));
    }

    private void applyPostgresUrl(String databaseUrl, ConfigurableEnvironment environment, Map<String, Object> properties) {
        URI uri = URI.create(databaseUrl);
        if (hasInvalidDatabasePath(uri.getPath())) {
            return;
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost());

        if (uri.getPort() > 0) {
            jdbcUrl.append(":").append(uri.getPort());
        }

        jdbcUrl.append(uri.getPath());

        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            jdbcUrl.append("?").append(uri.getQuery());
        }

        properties.put("spring.datasource.url", jdbcUrl.toString());

        String userInfo = uri.getRawUserInfo();
        if (userInfo == null || userInfo.isBlank()) {
            return;
        }

        String[] credentials = userInfo.split(":", 2);
        if (isBlank(environment.getProperty("DATABASE_USERNAME")) && isBlank(environment.getProperty("SPRING_DATASOURCE_USERNAME"))) {
            properties.put("spring.datasource.username", decode(credentials[0]));
        }

        if (credentials.length > 1
                && isBlank(environment.getProperty("DATABASE_PASSWORD"))
                && isBlank(environment.getProperty("SPRING_DATASOURCE_PASSWORD"))) {
            properties.put("spring.datasource.password", decode(credentials[1]));
        }
    }

    private void putIfAvailable(Map<String, Object> properties, String key, String value, String explicitValue, String springValue) {
        if (!isBlank(value) && isBlank(explicitValue) && isBlank(springValue)) {
            properties.put(key, value);
        }
    }

    private boolean hasInvalidJdbcDatabaseName(String jdbcUrl) {
        int queryStart = jdbcUrl.indexOf('?');
        String withoutQuery = queryStart >= 0 ? jdbcUrl.substring(0, queryStart) : jdbcUrl;
        int lastSlash = withoutQuery.lastIndexOf('/');
        String path = lastSlash >= 0 ? withoutQuery.substring(lastSlash) : "";
        return hasInvalidDatabasePath(path);
    }

    private boolean hasInvalidDatabasePath(String path) {
        if (isBlank(path) || "/".equals(path)) {
            return true;
        }

        String databaseName = path.startsWith("/") ? path.substring(1) : path;
        return databaseName.isBlank()
                || "$".equals(databaseName)
                || databaseName.contains("${")
                || databaseName.contains("{{")
                || databaseName.contains("}}");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
