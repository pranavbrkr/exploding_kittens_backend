package com.kitten.lobby.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.kitten.lobby.model.Lobby;
import com.kitten.lobby.service.LobbyService;

@RestController
@RequestMapping("/lobby")
public class LobbyWebSocketController {
  
  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @Autowired
  private LobbyService lobbyService;

  @Autowired
  private RestTemplate restTemplate;

  @PostMapping("/start/{lobbyId}")
  public void startGame(@PathVariable("lobbyId") String lobbyId) {
    Lobby lobby = lobbyService.getLobbyById(lobbyId);
    if (lobby == null) {
      throw new RuntimeException("Lobby not found");
    }

    // Fetch player names from player service
    List<String> playerNames = new ArrayList<>();
    for (String playerId : lobby.getPlayerIds()) {
      try {
        String playerUrl = "http://localhost:8080/player/" + playerId;
        PlayerResponse playerResponse = restTemplate.getForObject(playerUrl, PlayerResponse.class);
        if (playerResponse != null) {
          playerNames.add(playerResponse.getName());
        } else {
          playerNames.add("Player " + playerId);
        }
      } catch (Exception e) {
        // If player service is not available, use fallback name
        playerNames.add("Player " + playerId);
      }
    }

    // Create request body with both player IDs and names
    GameStartRequest gameStartRequest = new GameStartRequest();
    gameStartRequest.setPlayerIds(lobby.getPlayerIds());
    gameStartRequest.setPlayerNames(playerNames);

    String url = "http://localhost:8082/game/start?lobbyId=" + lobbyId;
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<GameStartRequest> request = new HttpEntity<>(gameStartRequest, headers);
    restTemplate.postForEntity(url, request, String.class);

    messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, "gameStarted");
  }

  // Inner classes for request/response
  public static class PlayerResponse {
    private String playerId;
    private String name;

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  public static class GameStartRequest {
    private List<String> playerIds;
    private List<String> playerNames;

    public List<String> getPlayerIds() { return playerIds; }
    public void setPlayerIds(List<String> playerIds) { this.playerIds = playerIds; }
    public List<String> getPlayerNames() { return playerNames; }
    public void setPlayerNames(List<String> playerNames) { this.playerNames = playerNames; }
  }
}
