package ch.uzh.ifi.hase.soprafs24.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.*;

import ch.uzh.ifi.hase.soprafs24.api.apiToken;


@Service
@Transactional
public class WordGenerationService {
  private final Logger log = LoggerFactory.getLogger(WordGenerationService.class);
  
  private static String API_KEY = apiToken.getApiToken();
  private static String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
  
  //fallback method to version without theme 
  public List<String> getWordsFromApi(){
    return getWordsFromApi(null); 
  }

  public List<String> getWordsFromApi(String theme) {
    int maxRetries = 5;
    for (int attempt = 0; attempt < maxRetries; attempt++){
      try {
        HttpClient client = HttpClient.newHttpClient(); 
        String prompt;

        if (theme != null && !theme.isBlank()) {
          prompt = "Give me a list of 25 random, common German nouns suitable for the board game Codenames. " +
                   "The words should all clearly relate to the theme: '" + theme + "'. Output them in a JSON array.";
      } else {
          prompt = "Give me a list of 25 random, common German nouns suitable for the board game Codenames. " +
                   "Output them in a JSON array.";
      }

        JsonObject messagePart = new JsonObject(); 
        messagePart.addProperty("text", prompt); 
        JsonArray parts = new JsonArray();
        parts.add(messagePart); 
  
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        JsonArray contents = new JsonArray(); 
        contents.add(content); 
  
        JsonObject requestBody = new JsonObject(); 
        requestBody.add("contents", contents); 
  
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
  
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString()); 
  
        JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject(); 
        String rawJsonArray = responseJson
                      .getAsJsonArray("candidates")
                      .get(0).getAsJsonObject()
                      .getAsJsonObject("content")
                      .getAsJsonArray("parts")
                      .get(0).getAsJsonObject()
                      .get("text").getAsString();
  
        if (rawJsonArray.contains("```")){
          rawJsonArray = rawJsonArray
              .replaceAll("(?s)```json", "")
              .replaceAll("(?s)```", "")
              .trim();
        }
  
        JsonArray wordArray = JsonParser.parseString(rawJsonArray).getAsJsonArray();
  
        List<String> words = new ArrayList<>();
        Set<String> seenWords = new HashSet<>();

        for (JsonElement word : wordArray) {
          String w = word.getAsString().trim().toUpperCase();
          if(!seenWords.contains(w)){
            seenWords.add(w);
            words.add(w);
          }
        }

        if (words.size() == 25) {
          return words;
        } else {
          log.warn("Attempt {}: Duplicate words found, retrying...", attempt+1);
        }
        
      } catch (Exception e) {
        log.error("Error fetching words from Gemini", e);
        return List.of(); // fallback: just return an empty list
      }
    }
    log.error("Exceed maximum retries"); 
    return List.of();
  }

  public void setApiKey(String apiKey) {
    API_KEY = apiKey; 
    ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
  }
}
