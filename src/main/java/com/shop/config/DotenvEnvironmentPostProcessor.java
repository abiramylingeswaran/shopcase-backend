package com.shop.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads backend/.env into the Spring Environment so IntelliJ / IDE runs
 * pick up DB_PASSWORD and other secrets without manual run-config setup.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path envFile = resolveEnvFile();
        if (envFile == null || !Files.isRegularFile(envFile)) {
            return;
        }

        try {
            Map<String, Object> values = new LinkedHashMap<>();
            for (String raw : Files.readAllLines(envFile)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int idx = line.indexOf('=');
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                // Prefer OS/IDE env vars; otherwise load from .env
                if (System.getenv(key) == null) {
                    values.put(key, value);
                    // Also bind common WhatsApp keys as Spring property names
                    // so ConfigurationProperties works even if placeholders miss.
                    switch (key) {
                        case "WHATSAPP_PROVIDER" -> values.put("whatsapp.provider", value);
                        case "WHATSAPP_API_VERSION" -> values.put("whatsapp.api-version", value);
                        case "WHATSAPP_ACCESS_TOKEN" -> values.put("whatsapp.access-token", value);
                        case "WHATSAPP_PHONE_NUMBER_ID" -> values.put("whatsapp.phone-number-id", value);
                        case "WHATSAPP_BUSINESS_ACCOUNT_ID" -> values.put("whatsapp.business-account-id", value);
                        case "WHATSAPP_TEMPLATE_NAME" -> values.put("whatsapp.template-name", value);
                        case "WHATSAPP_TEMPLATE_LANGUAGE" -> values.put("whatsapp.template-language", value);
                        case "WHATSAPP_ALLOW_TEST_FALLBACK" ->
                                values.put("whatsapp.allow-test-fallback", value);
                        case "STORE_WHATSAPP_NUMBER", "WHATSAPP_STORE_NUMBER" ->
                                values.put("whatsapp.store-number", value);
                        case "WHATSAPP_DEFAULT_COUNTRY_CODE" ->
                                values.put("whatsapp.default-country-code", value);
                        case "WHATSAPP_SEND_CUSTOMER_INVOICE" ->
                                values.put("whatsapp.send-customer-invoice", value);
                        case "WHATSAPP_SEND_STORE_NOTIFICATION" ->
                                values.put("whatsapp.send-store-notification", value);
                        case "WHATSAPP_MANAGED_NODE" -> values.put("whatsapp.managed-node", value);
                        case "WHATSAPP_SERVICE_DIR" -> values.put("whatsapp.service-dir", value);
                        case "WHATSAPP_STARTUP_TIMEOUT_SECONDS" ->
                                values.put("whatsapp.startup-timeout-seconds", value);
                        case "WHATSAPP_AUTO_NPM_INSTALL" ->
                                values.put("whatsapp.auto-npm-install", value);
                        default -> { /* no-op */ }
                    }
                }
            }
            if (!values.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource("dotenv", values));
            }
        } catch (IOException ignored) {
            // If .env cannot be read, fall back to application.yml defaults
        }
    }

    private Path resolveEnvFile() {
        Path cwd = Path.of("").toAbsolutePath();
        Path direct = cwd.resolve(".env");
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        Path backend = cwd.resolve("backend").resolve(".env");
        if (Files.isRegularFile(backend)) {
            return backend;
        }
        return null;
    }
}
