package com.kitten.lobby.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kitten.lobby.client.PlayerClient;
import com.kitten.lobby.dto.LobbyCreateRequest;
import com.kitten.lobby.dto.LobbyJoinRequest;
import com.kitten.lobby.dto.LobbyResponse;
import com.kitten.lobby.model.Lobby;
import com.kitten.lobby.service.LobbyService;

@RestController
@RequestMapping("/lobby")
@CrossOrigin
public class LobbyController {
  @Autowired
  private LobbyService lobbyService;

  @Autowired
  private PlayerClient playerClient;

  @PostMapping("/create")
  public LobbyResponse createLobby(@RequestBody LobbyCreateRequest request) {
    String playerId = request.getPlayerId();

    if(!playerClient.isValidPlayer(playerId)) {
      throw new RuntimeException("Invalid player ID");
    }

    Lobby lobby = lobbyService.createLobby(playerId);
    return new LobbyResponse(lobby.getLobbyId(), lobby.getPlayerIds());
  }

  @PostMapping("/join")
  public LobbyResponse joinLobby(@RequestBody LobbyJoinRequest request) {
    String playerId = request.getPlayerId();
    String lobbyId = request.getLobbyId();

    if (!playerClient.isValidPlayer(playerId)) {
      throw new RuntimeException("Invalid player ID");
    }

    if (!lobbyService.lobbyExists(lobbyId)) {
      throw new RuntimeException("Lobby not found");
    }

    lobbyService.addPlayerToLobby(lobbyId, playerId);
    Lobby updated = lobbyService.getLobbyById(lobbyId);
    return new LobbyResponse(updated.getLobbyId(), updated.getPlayerIds());
  }
}
