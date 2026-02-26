package com.kitten.player.dto;

/** Step 2: verify email with the 6-digit code sent to the user. */
public class VerifyEmailRequest {
  private String email;
  private String code;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }
}
