package com.kitten.lobby.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Lobby {
  private String lobbyId;
  private List<String> playerIds;

  public Lobby(String hostPlayerId) {
    this.lobbyId = UUID.randomUUID().toString().substring(0, 6);
    this.playerIds = new ArrayList<>();
    this.playerIds.add(hostPlayerId);
  }

  public String getLobbyId() {
    return lobbyId;
  }

  public List<String> getPlayerIds() {
    return playerIds;
  }
}
