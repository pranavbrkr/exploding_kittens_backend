package com.kitten.player.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  /** Unique; used for login. One player per email. */
  @Column(unique = true)
  private String email;

  /** Game name (unique), set after email verification. Null until complete-registration. */
  @Column(unique = true)
  private String username;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "verification_code", length = 6)
  private String verificationCode;

  @Column(name = "verification_code_expires_at")
  private Instant verificationCodeExpiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public User() {}

  /** For new registration: email + password only; emailVerified=false, username=null. */
  public User(String email, String passwordHash) {
    this.id = UUID.randomUUID();
    this.email = email != null ? email.trim().toLowerCase() : null;
    this.username = null;
    this.displayName = null;
    this.passwordHash = passwordHash;
    this.emailVerified = false;
    this.verificationCode = null;
    this.verificationCodeExpiresAt = null;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public String getVerificationCode() {
    return verificationCode;
  }

  public void setVerificationCode(String verificationCode) {
    this.verificationCode = verificationCode;
  }

  public Instant getVerificationCodeExpiresAt() {
    return verificationCodeExpiresAt;
  }

  public void setVerificationCodeExpiresAt(Instant verificationCodeExpiresAt) {
    this.verificationCodeExpiresAt = verificationCodeExpiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Display name for lobby/game: game name if set, else email prefix. */
  public String getEffectiveDisplayName() {
    if (displayName != null && !displayName.isBlank()) return displayName;
    if (username != null && !username.isBlank()) return username;
    if (email != null && email.contains("@")) return email.substring(0, email.indexOf("@"));
    return "Player";
  }
}
