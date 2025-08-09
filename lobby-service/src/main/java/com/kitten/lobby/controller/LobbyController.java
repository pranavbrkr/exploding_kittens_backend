package com.kitten.lobby.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.kitten.lobby.client.PlayerClient;
import com.kitten.lobby.dto.LobbyCreateRequest;
import com.kitten.lobby.dto.LobbyJoinRequest;
import com.kitten.lobby.dto.LobbyResponse;
import com.kitten.lobby.dto.PlayerResponse;
import com.kitten.lobby.model.Lobby;
import com.kitten.lobby.service.LobbyService;

@RestController
@RequestMapping("/api/lobby")
@CrossOrigin
public class LobbyController {
  @Autowired
  private LobbyService lobbyService;

  @Autowired
  private PlayerClient playerClient;

  @GetMapping("/{lobbyId}")
  public LobbyResponse getLobby(@PathVariable("lobbyId") String lobbyId) {
    Lobby lobby = lobbyService.getLobbyById(lobbyId);
    if (lobby == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
    }

    List<String> safeCopy = List.copyOf(lobby.getPlayerIds());
    List<PlayerResponse> playerList = safeCopy.stream()
      .map(playerClient::getPlayerDetails)
      .toList();
    return new LobbyResponse(lobby.getLobbyId(), playerList);
  }

  @PostMapping("/create")
  public LobbyResponse createLobby(@RequestBody LobbyCreateRequest request) {
    String playerId = request.getPlayerId();

    if(!playerClient.isValidPlayer(playerId)) {
      throw new RuntimeException("Invalid player ID");
    }

    Lobby lobby = lobbyService.createLobby(playerId);
    List<PlayerResponse> playerList = lobby.getPlayerIds().stream()
      .map(playerClient::getPlayerDetails)
      .toList();

      return new LobbyResponse(lobby.getLobbyId(), playerList);
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

    if (lobbyService.isLobbyFull(lobbyId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lobby is full");
    }

    lobbyService.addPlayerToLobby(lobbyId, playerId);
    Lobby updated = lobbyService.getLobbyById(lobbyId);

    List<PlayerResponse> playerList = updated.getPlayerIds().stream()
      .map(playerClient::getPlayerDetails)
      .toList();

      return new LobbyResponse(updated.getLobbyId(), playerList);
  }
}
