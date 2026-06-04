package persistence;

import java.util.HashMap;
import java.util.Map;

public final class DatabaseConfig {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/mas_university";
    private static final String DEFAULT_USER = "mas_user";
    private static final String DEFAULT_PASSWORD = "mas_password";

    private DatabaseConfig() {
    }

    public static String jdbcUrl() {
        return read("MAS_DB_URL", DEFAULT_URL);
    }

    public static String username() {
        return read("MAS_DB_USER", DEFAULT_USER);
    }

    public static String password() {
        return read("MAS_DB_PASSWORD", DEFAULT_PASSWORD);
    }

    public static Map<String, Object> jpaProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", jdbcUrl());
        properties.put("jakarta.persistence.jdbc.user", username());
        properties.put("jakarta.persistence.jdbc.password", password());
        return properties;
    }

    private static String read(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
