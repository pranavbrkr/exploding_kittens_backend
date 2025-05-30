package com.kitten.lobby.controller;

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

    String url = "http://localhost:8082/game/start?lobbyId=" + lobbyId;
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<List<String>> request = new HttpEntity<>(lobby.getPlayerIds(), headers);
    restTemplate.postForEntity(url, request, String.class);

    messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, "gameStarted");
  }
}
