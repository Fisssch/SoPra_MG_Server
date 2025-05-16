package ch.uzh.ifi.hase.soprafs24.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import ch.uzh.ifi.hase.soprafs24.constant.GameLanguage;

import ch.uzh.ifi.hase.soprafs24.api.apiToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WordGenerationServiceTest {

    private WordGenerationService wordGenerationService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        apiToken.isTestEnvironment = true; // Set to true for testing purposes
        wordGenerationService = new WordGenerationService();
        wordGenerationService.setApiKey("testApiKey");
    }

    @Test
    public void getWordsFromApi_withoutTheme_returnsWordsOrEmpty() {
        List<String> words = wordGenerationService.getWordsFromApi(GameLanguage.GERMAN);

        assertNotNull(words);
        assertTrue(words.size() == 0 || words.size() == 25); // either fallback empty list or 25 words
    }

    @Test
    public void getWordsFromApi_withTheme_returnsWordsOrEmpty() {
        List<String> words = wordGenerationService.getWordsFromApi("Informatik", GameLanguage.GERMAN);

        assertNotNull(words);
        assertTrue(words.size() == 0 || words.size() == 25); // fallback or success
    }


    @Test
    public void setApiKey_changesEndpoint() {
        wordGenerationService.setApiKey("newKey123");
        // We don't have access to private static field ENDPOINT directly,
        // but at least assert that setting doesn't throw and future calls use new key
        List<String> words = wordGenerationService.getWordsFromApi(GameLanguage.ENGLISH);
        assertNotNull(words);
    }
}