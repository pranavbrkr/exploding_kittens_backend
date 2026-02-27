package com.kitten.lobby.dto;

public class LobbyJoinRequest {
  private String lobbyId;
  private String playerId;

  public String getLobbyId() {
    return lobbyId;
  }

  public String getPlayerId() {
    return playerId;
  }

  public void setLobbyId(String lobbyId) {
    this.lobbyId = lobbyId;
  }

  public void setPlayerId(String playerId) {
    this.playerId = playerId;
  }
}
