package com.experimentalassistant.backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DotEnvLoader {

    private DotEnvLoader() {
    }

    public static void loadIfPresent() {
        if (Boolean.parseBoolean(System.getProperty("DOTENV_DISABLE", "false"))) {
            return;
        }

        Path envPath = resolveEnvPath();
        if (envPath == null) {
            return;
        }

        Map<String, String> env;
        try {
            env = parseEnvFile(envPath);
        } catch (IOException e) {
            return;
        }

        applyCommon(env);
        applyAi(env);
    }

    private static Path resolveEnvPath() {
        Path cwdEnv = Paths.get(".env").toAbsolutePath().normalize();
        if (Files.isRegularFile(cwdEnv)) {
            return cwdEnv;
        }

        Path backendEnv = Paths.get("backend", ".env").toAbsolutePath().normalize();
        if (Files.isRegularFile(backendEnv)) {
            return backendEnv;
        }

        return null;
    }

    private static Map<String, String> parseEnvFile(Path envPath) throws IOException {
        List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
        Map<String, String> map = new LinkedHashMap<>();
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            if (value.length() >= 2) {
                char first = value.charAt(0);
                char last = value.charAt(value.length() - 1);
                if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                    value = value.substring(1, value.length() - 1);
                }
            }
            if (!key.isEmpty() && !map.containsKey(key)) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static void applyCommon(Map<String, String> env) {
        setSystemPropertyIfAbsent("DB_URL", env.get("DB_URL"));
        setSystemPropertyIfAbsent("DB_USERNAME", env.get("DB_USERNAME"));
        setSystemPropertyIfAbsent("DB_PASSWORD", env.get("DB_PASSWORD"));
        setSystemPropertyIfAbsent("DB_INIT_MODE", env.get("DB_INIT_MODE"));
    }

    private static void applyAi(Map<String, String> env) {
        String aiEnable = firstNonBlank(env.get("AI_ENABLE"), env.get("AI_ENABLED"));
        if (aiEnable != null) {
            setSystemPropertyIfAbsent("ai.enable", aiEnable);
        }

        String provider = firstNonBlank(env.get("AI_PROVIDER"), env.get("AI_HTTP_PROVIDER"));
        if (provider != null) {
            setSystemPropertyIfAbsent("ai.provider", provider);
        } else if (aiEnable != null && Boolean.parseBoolean(aiEnable)) {
            setSystemPropertyIfAbsent("ai.provider", "http");
        }

        String baseUrl = firstNonBlank(env.get("AI_HTTP_BASE_URL"), env.get("AI_BASE_URL"));
        setSystemPropertyIfAbsent("ai.http.base-url", baseUrl);

        String apiKey = firstNonBlank(env.get("AI_HTTP_API_KEY"), env.get("AI_API_KEY"));
        setSystemPropertyIfAbsent("ai.http.api-key", apiKey);

        String model = firstNonBlank(env.get("AI_HTTP_MODEL"), env.get("AI_MODEL"));
        setSystemPropertyIfAbsent("ai.http.model", model);

        String timeoutMs = firstNonBlank(env.get("AI_HTTP_TIMEOUT_MS"), env.get("AI_TIMEOUT_MS"));
        setSystemPropertyIfAbsent("ai.http.timeout-ms", timeoutMs);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static void setSystemPropertyIfAbsent(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (System.getProperty(key) != null) {
            return;
        }
        System.setProperty(key, value);
    }
}

