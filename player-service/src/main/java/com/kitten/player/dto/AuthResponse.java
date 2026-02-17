package com.kitten.player.dto;

public class AuthResponse {
  private String token;
  private String playerId;
  private String name;

  public AuthResponse(String token, String playerId, String name) {
    this.token = token;
    this.playerId = playerId;
    this.name = name;
  }

  public String getToken() {
    return token;
  }

  public String getPlayerId() {
    return playerId;
  }

  public String getName() {
    return name;
  }
}
