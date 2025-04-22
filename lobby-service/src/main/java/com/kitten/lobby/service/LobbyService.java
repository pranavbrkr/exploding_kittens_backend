package com.kitten.lobby.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kitten.lobby.model.Lobby;

@Service
public class LobbyService {
  private final Map<String, Lobby> lobbyStore = new HashMap<>();

  public Lobby createLobby(String playerId) {
    Lobby lobby = new Lobby(playerId);
    lobbyStore.put(lobby.getLobbyId(), lobby);
    return lobby;
  }

  public Lobby getLobbyById(String lobbyId) {
    return lobbyStore.get(lobbyId);
  }

  public boolean lobbyExists(String lobbyId) {
    return lobbyStore.containsKey(lobbyId);
  }

  public void addPlayerToLobby(String lobbyId, String playerId) {
    Lobby lobby = lobbyStore.get(lobbyId);
    if (lobby != null) {
      lobby.getPlayerIds().add(playerId);
    }
  }
}
