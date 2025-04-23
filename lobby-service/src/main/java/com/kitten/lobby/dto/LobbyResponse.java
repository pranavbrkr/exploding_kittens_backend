package com.kitten.lobby.dto;

import java.util.List;

public class LobbyResponse {
  private String lobbyId;
  private List<PlayerResponse> players;

  public LobbyResponse(String lobbyId, List<PlayerResponse> players) {
    this.lobbyId = lobbyId;
    this.players = players;
  }

  public String getLobbyId() {
    return lobbyId;
  }

  public List<PlayerResponse> getPlayers() {
    return players;
  }

  public void setLobbyId(String lobbyId) {
    this.lobbyId = lobbyId;
  }

  public void setPlayers(List<PlayerResponse> players) {
    this.players = players;
  }
}
