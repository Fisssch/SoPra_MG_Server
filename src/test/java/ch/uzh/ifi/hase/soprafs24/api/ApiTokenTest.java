package ch.uzh.ifi.hase.soprafs24.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiTokenTest {

    private String originalApiKey;
    private Map<String, String> modifiedEnv;

    @BeforeEach
    void setUp() throws Exception {
        // Save the original API_KEY environment variable if it exists
        originalApiKey = System.getenv("API_KEY");
        File localFile = new File("local.properties");
        if (localFile.exists()) {
            localFile.renameTo(new File("local.properties.bak"));
        }
                
        // Get the environment variables map for modification
        modifiedEnv = getModifiableEnvironmentVariables();

        modifiedEnv.remove("API_KEY"); // Remove the API_KEY to start fresh
    }

    @AfterEach
    void tearDown() throws Exception {
        // Restore the original environment
        if (originalApiKey != null) {
            modifiedEnv.put("API_KEY", originalApiKey);
        } else {
            modifiedEnv.remove("API_KEY");
        }
        File localFile = new File("local.properties.bak");
        if (localFile.exists()) {
            localFile.renameTo(new File("local.properties"));
        }
    }

    @Test
    void getApiToken_WithoutEnvVariableOrPropertiesFile_ShouldThrowException() throws Exception {
        // Arrange
        modifiedEnv.remove("API_KEY");
        apiToken.isTestEnvironment = false;
        
        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> apiToken.getApiToken()
        );
        assertTrue(exception.getMessage().contains("API_KEY not found"));
    }

    @Test
    void getApiToken_InTestEnvironment_ShouldReturnNull() throws Exception {
        // Arrange
        modifiedEnv.remove("API_KEY");
        apiToken.isTestEnvironment = true;
        
        // Act
        String actualToken = apiToken.getApiToken();
        
        // Assert
        assertNull(actualToken); // In test environment, null is acceptable
    }

    // Helper methods

    @SuppressWarnings("unchecked")
    private Map<String, String> getModifiableEnvironmentVariables() throws Exception {
        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);
        return (Map<String, String>) theEnvironmentField.get(null);
    }
}
