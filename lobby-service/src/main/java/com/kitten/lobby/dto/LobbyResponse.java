package com.kitten.lobby.dto;

import java.util.List;

public class LobbyResponse {
  private String lobbyId;
  private List<String> playerIds;

  public LobbyResponse(String lobbyId, List<String> playerIds) {
    this.lobbyId = lobbyId;
    this.playerIds = playerIds;
  }

  public String getLobbyId() {
    return lobbyId;
  }

  public List<String> getPlayerIds() {
    return playerIds;
  }
}
