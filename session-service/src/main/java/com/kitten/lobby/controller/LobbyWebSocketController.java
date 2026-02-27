package com.kitten.lobby.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.kitten.lobby.model.Lobby;
import com.kitten.lobby.service.LobbyService;
import com.kitten.player.dto.PlayerResponse;
import com.kitten.player.service.PlayerService;

@RestController
@RequestMapping("/api/lobby")
public class LobbyWebSocketController {

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @Autowired
  private LobbyService lobbyService;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private RestTemplate restTemplate;

  @Value("${game.service.url:http://localhost:8082}")
  private String gameServiceUrl;

  @PostMapping("/start/{lobbyId}")
  public void startGame(@PathVariable("lobbyId") String lobbyId) {
    Lobby lobby = lobbyService.getLobbyById(lobbyId);
    if (lobby == null) {
      throw new RuntimeException("Lobby not found");
    }

    List<String> playerNames = new ArrayList<>();
    for (String playerId : lobby.getPlayerIds()) {
      PlayerResponse player = playerService.getPlayerById(playerId);
      playerNames.add(player != null ? player.getName() : "Player " + playerId);
    }

    GameStartRequest gameStartRequest = new GameStartRequest();
    gameStartRequest.setPlayerIds(lobby.getPlayerIds());
    gameStartRequest.setPlayerNames(playerNames);

    String url = gameServiceUrl + "/api/game/start?lobbyId=" + lobbyId;
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<GameStartRequest> request = new HttpEntity<>(gameStartRequest, headers);
    restTemplate.postForEntity(url, request, String.class);

    messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, "gameStarted");
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
