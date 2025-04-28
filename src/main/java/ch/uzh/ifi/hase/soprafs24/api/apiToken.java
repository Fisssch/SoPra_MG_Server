package ch.uzh.ifi.hase.soprafs24.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Paths;

public class apiToken {
    public static String getApiToken() {        
        String apiKey = null;
        try {
            Properties properties = new Properties();
            String propertiesPath = Paths.get(System.getProperty("user.dir"), "local.properties").toString();
            properties.load(new FileInputStream(propertiesPath));
            apiKey = properties.getProperty("API_KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                return apiKey;
            }
        } catch (IOException e) { }
        // Fall back to environment variable
        try {
            apiKey = System.getenv("API_KEY");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Failed to load api key");
        }
        if (apiKey != null && !apiKey.isEmpty())
            return apiKey;
        throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "API_KEY not found in environment variables or local.properties file");
    }
}