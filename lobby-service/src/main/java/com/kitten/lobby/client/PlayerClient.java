package com.kitten.lobby.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.kitten.lobby.dto.PlayerResponse;

@Component
public class PlayerClient {
  private final WebClient webClient;

  public PlayerClient(WebClient.Builder builder) {
    this.webClient = builder.baseUrl("http://localhost:8080").build();
  }

  public boolean isValidPlayer(String playerId) {
    try {
      return this.webClient.get()
          .uri("/player/{id}", playerId)
          .retrieve()
          .bodyToMono(Object.class)
          .block() != null;
    } catch (Exception e) {
      System.out.println("Player lookup failed: " + e.getMessage());
      return false;
    }
  }

  public PlayerResponse getPlayerDetails(String playerId) {
    try {
      return this.webClient.get()
        .uri("/player/{id}", playerId)
        .retrieve()
        .bodyToMono(PlayerResponse.class)
        .block();
    } catch(Exception e) {
      System.out.println("Failed to get player details: " + e.getMessage());
      return null;
    }
  }
}
