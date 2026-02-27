package com.kitten.player.dto;

public class AuthResponse {
  private String token;
  private String playerId;
  private String name;
  /** True when user just verified email and must set game name (complete-registration). */
  private boolean needsGameName;

  public AuthResponse(String token, String playerId, String name) {
    this(token, playerId, name, false);
  }

  public AuthResponse(String token, String playerId, String name, boolean needsGameName) {
    this.token = token;
    this.playerId = playerId;
    this.name = name;
    this.needsGameName = needsGameName;
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

  public boolean isNeedsGameName() {
    return needsGameName;
  }
}
