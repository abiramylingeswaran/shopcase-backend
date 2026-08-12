package com.shop.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps Railway-style {@code DATABASE_URL} / {@code PORT} into Spring Boot properties.
 * Also overrides accidental localhost {@code DB_URL} values when Railway Postgres is present.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RailwayEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> values = new LinkedHashMap<>();

        String port = firstEnv("PORT", "SERVER_PORT");
        if (StringUtils.hasText(port)) {
            values.put("server.port", port);
        }

        String explicitDbUrl = System.getenv("DB_URL");
        boolean localhostDb = isLocalhostDb(explicitDbUrl);
        boolean shouldMapRailwayDb = !StringUtils.hasText(explicitDbUrl) || localhostDb;

        if (shouldMapRailwayDb) {
            String databaseUrl = firstEnv("DATABASE_URL", "POSTGRES_URL", "POSTGRES_PRIVATE_URL");
            if (StringUtils.hasText(databaseUrl) && !isLocalhostDb(databaseUrl)) {
                try {
                    applyDatabaseUrl(databaseUrl, values);
                } catch (Exception ignored) {
                    // Leave defaults; startup will fail with a clear datasource error
                }
            } else {
                String host = firstEnv("PGHOST", "POSTGRES_HOST");
                String pgPort = firstEnv("PGPORT", "POSTGRES_PORT");
                String db = firstEnv("PGDATABASE", "POSTGRES_DB");
                String user = firstEnv("PGUSER", "POSTGRES_USER");
                String password = firstEnv("PGPASSWORD", "POSTGRES_PASSWORD");
                if (StringUtils.hasText(host) && StringUtils.hasText(db) && !isLocalhostHost(host)) {
                    String jdbcPort = StringUtils.hasText(pgPort) ? pgPort : "5432";
                    String jdbc = "jdbc:postgresql://" + host + ":" + jdbcPort + "/" + db;
                    putDatasource(values, jdbc, user, password);
                }
            }
        }

        if (!values.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("railway", values));
        }
    }

    private static void applyDatabaseUrl(String raw, Map<String, Object> values) throws Exception {
        String normalized = raw.trim();
        if (normalized.startsWith("jdbc:")) {
            // Already JDBC — still extract user/pass if present in URI form after jdbc:
            // jdbc:postgresql://user:pass@host:port/db is uncommon; prefer plain URL parse below.
            if (normalized.startsWith("jdbc:postgresql://") || normalized.startsWith("jdbc:postgres://")) {
                normalized = normalized.substring("jdbc:".length());
            } else {
                values.put("DB_URL", raw.trim());
                values.put("spring.datasource.url", raw.trim());
                return;
            }
        }
        if (normalized.startsWith("postgres://")) {
            normalized = "postgresql://" + normalized.substring("postgres://".length());
        }
        URI uri = URI.create(normalized);
        String userInfo = uri.getUserInfo();
        String user = null;
        String password = null;
        if (StringUtils.hasText(userInfo)) {
            int idx = userInfo.indexOf(':');
            if (idx >= 0) {
                user = decode(userInfo.substring(0, idx));
                password = decode(userInfo.substring(idx + 1));
            } else {
                user = decode(userInfo);
            }
        }
        String path = uri.getPath();
        String database = StringUtils.hasText(path) && path.startsWith("/") ? path.substring(1) : path;
        // Strip query params from db name if present
        if (database != null && database.contains("?")) {
            database = database.substring(0, database.indexOf('?'));
        }
        int dbPort = uri.getPort() > 0 ? uri.getPort() : 5432;
        String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + dbPort + "/" + database;
        putDatasource(values, jdbc, user, password);
    }

    private static void putDatasource(Map<String, Object> values, String jdbc, String user, String password) {
        values.put("DB_URL", jdbc);
        values.put("spring.datasource.url", jdbc);
        if (StringUtils.hasText(user)) {
            values.put("DB_USERNAME", user);
            values.put("spring.datasource.username", user);
        }
        if (StringUtils.hasText(password)) {
            values.put("DB_PASSWORD", password);
            values.put("spring.datasource.password", password);
        }
    }

    private static boolean isLocalhostDb(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.contains("localhost") || lower.contains("127.0.0.1");
    }

    private static boolean isLocalhostHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }
        String lower = host.toLowerCase();
        return "localhost".equals(lower) || "127.0.0.1".equals(lower);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstEnv(String... keys) {
        for (String key : keys) {
            String value = System.getenv(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
