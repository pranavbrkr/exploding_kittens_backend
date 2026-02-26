package com.kitten.player.dto;

/** Step 3: set unique game name (requires JWT from verify-email). */
public class CompleteRegistrationRequest {
  private String gameName;

  public String getGameName() {
    return gameName;
  }

  public void setGameName(String gameName) {
    this.gameName = gameName;
  }
}
