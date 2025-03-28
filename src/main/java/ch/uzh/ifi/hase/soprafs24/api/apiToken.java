package ch.uzh.ifi.hase.soprafs24.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Paths;

public class apiToken {
    public static String getApiToken() {        
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
        
        // Fall back to environment variable
        try {
            return System.getenv("API_KEY");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load api key");
        }
    }
}