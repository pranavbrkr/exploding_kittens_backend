package com.kitten.lobby.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lobby")
public class LobbyWebSocketController {
  
  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  @PostMapping("/start/{lobbyId}")
  public void startGame(@PathVariable("lobbyId") String lobbyId) {
    messagingTemplate.convertAndSend("/topic/lobby/" + lobbyId, "gameStarted");
  }
}
