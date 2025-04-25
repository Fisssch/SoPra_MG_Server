package ch.uzh.ifi.hase.soprafs24.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest;
import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class apiToken {

    public static String getApiToken() {
        //getting from local.properties 
        try {
            Properties properties = new Properties();
            String path = Paths.get(System.getProperty("user.dir"), "local.properties").toString();
            properties.load(new FileInputStream(path));
            String apiKey = properties.getProperty("API_KEY");
            if (apiKey != null && !apiKey.isEmpty()) {
                return apiKey;
            }
        } catch (IOException ignored) {
            //missing file 
        }

        //getting from secret manager
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            String secretName = "projects/1075428158089/secrets/API_KEY/versions/latest";
            AccessSecretVersionRequest request = AccessSecretVersionRequest.newBuilder()
                    .setName(secretName)
                    .build();
            AccessSecretVersionResponse response = client.accessSecretVersion(request);
            return response.getPayload().getData().toStringUtf8();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load API key from Secret Manager", e);
        }
    }
}
