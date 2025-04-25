package ch.uzh.ifi.hase.soprafs24.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import java.nio.file.Paths;

public class apiToken {
    public static String getLocalApiToken() {        
        try {
            Properties properties = new Properties();
            String propertiesPath = Paths.get(System.getProperty("user.dir"), "local.properties").toString();
            properties.load(new FileInputStream(propertiesPath));
            String apiKey = properties.getProperty("API_KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                return apiKey;
            }
        } catch (IOException e) {
        }
        return null;
    }
}