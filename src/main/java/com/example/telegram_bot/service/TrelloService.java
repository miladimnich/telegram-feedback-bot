package com.example.telegram_bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service

public class TrelloService {
    private final String API_KEY;
    private final String TOKEN;
    private final String LIST_ID;

    public TrelloService(@Value("${trello.api}") String apiKey,
                         @Value("${trello.token}") String token,
                         @Value("${trello.listId}") String listId) {
        API_KEY = apiKey;
        TOKEN = token;
        LIST_ID = listId;
    }

    public void createCard(String name, String description) throws IOException, InterruptedException {
        String url = String.format(
                "https://api.trello.com/1/cards?key=%s&token=%s&idList=%s&name=%s&desc=%s",
                API_KEY,
                TOKEN,
                LIST_ID,
                URLEncoder.encode(name, StandardCharsets.UTF_8),
                URLEncoder.encode(description, StandardCharsets.UTF_8)
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody()) // параметри в URL
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            System.out.println("Trello card created successfully: " + response.body());
        } else {
            System.err.println("Failed to create Trello card. Status: " + response.statusCode());
            System.err.println("Response: " + response.body());
        }
    }

}
