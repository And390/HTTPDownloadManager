package ru.downloadmanager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class ApplicationConfig {

    public static Properties config;

    private static Properties getProperties() {
        if (config == null) {
            try (InputStream in = new FileInputStream("config.properties")) {
                config = new Properties();
                config.load(in);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Configuration file 'config.properties' isn't found", e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return config;
    }

    public static synchronized String get(String key) {
        return getProperties().getProperty(key);
    }

    public static synchronized String getRequired(String key) {
        String value = get(key);
        if (value == null) {
            throw new RuntimeException("No configuration parameter '" + key + "'");
        }
        if (value.isEmpty()) {
            throw new RuntimeException("Empty value for configuration parameter '" + key + "'");
        }
        return value;
    }

    public static synchronized int getRequiredInt(String key) {
        String value = getRequired(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Wrong integer value for configuration parameter '" + key + "': " + value);
        }
    }
}
