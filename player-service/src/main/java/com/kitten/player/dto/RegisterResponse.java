package com.kitten.player.dto;

/** Response after register (no JWT yet). */
public class RegisterResponse {
  private String message;

  public RegisterResponse(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
