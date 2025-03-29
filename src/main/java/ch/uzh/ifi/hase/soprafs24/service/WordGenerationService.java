package ch.uzh.ifi.hase.soprafs24.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

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
    
  private static final String API_KEY = apiToken.getApiToken();
  private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

  public List<String> getWordsFromApi() {
    try {
      HttpClient client = HttpClient.newHttpClient(); 
      String prompt = "Give me a list of 25 random, common German nouns suitable for the board game Codenames. Output them in a JSON array.";

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

      JsonArray wordArray;
      try {
        wordArray = JsonParser.parseString(rawJsonArray).getAsJsonArray();
      } catch (JsonSyntaxException e) {
        log.error("Couldn't parse response as JSON array: {}", rawJsonArray);
        return List.of();
      }

      List<String> words = new ArrayList<>();
      for (JsonElement word : wordArray) {
        words.add(word.getAsString());
      }

      return words;
    } catch (Exception e) {
        log.error("Error fetching words from Gemini", e);
        return List.of(); // fallback: just return an empty list
      }
  }
}
