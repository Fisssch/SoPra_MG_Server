package ch.uzh.ifi.hase.soprafs24.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WordGenerationServiceTest {

    private WordGenerationService wordGenerationService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        wordGenerationService = new WordGenerationService();
        wordGenerationService.setApiKey("testApiKey");
    }

    @Test
    public void getWordsFromApi_withoutTheme_returnsWordsOrEmpty() {
        List<String> words = wordGenerationService.getWordsFromApi();

        assertNotNull(words);
        assertTrue(words.size() == 0 || words.size() == 25); // either fallback empty list or 25 words
    }

    @Test
    public void getWordsFromApi_withTheme_returnsWordsOrEmpty() {
        List<String> words = wordGenerationService.getWordsFromApi("Informatik");

        assertNotNull(words);
        assertTrue(words.size() == 0 || words.size() == 25); // fallback or success
    }
    
}
