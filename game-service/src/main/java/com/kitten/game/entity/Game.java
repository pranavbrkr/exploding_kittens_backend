package com.kitten.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "games")
public class Game {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "lobby_id", unique = true, nullable = false)
  private String lobbyId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GameStatus status = GameStatus.ACTIVE;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "winner_user_id", columnDefinition = "uuid")
  private UUID winnerUserId;

  @Column(name = "seed")
  private Long seed;

  @Column(name = "rule_version")
  private String ruleVersion;

  public enum GameStatus {
    ACTIVE,
    FINISHED
  }

  public Game() {}

  public Game(UUID id, String lobbyId) {
    this.id = id;
    this.lobbyId = lobbyId;
    this.status = GameStatus.ACTIVE;
    this.startedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getLobbyId() {
    return lobbyId;
  }

  public void setLobbyId(String lobbyId) {
    this.lobbyId = lobbyId;
  }

  public GameStatus getStatus() {
    return status;
  }

  public void setStatus(GameStatus status) {
    this.status = status;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public Instant getEndedAt() {
    return endedAt;
  }

  public void setEndedAt(Instant endedAt) {
    this.endedAt = endedAt;
  }

  public UUID getWinnerUserId() {
    return winnerUserId;
  }

  public void setWinnerUserId(UUID winnerUserId) {
    this.winnerUserId = winnerUserId;
  }

  public Long getSeed() {
    return seed;
  }

  public void setSeed(Long seed) {
    this.seed = seed;
  }

  public String getRuleVersion() {
    return ruleVersion;
  }

  public void setRuleVersion(String ruleVersion) {
    this.ruleVersion = ruleVersion;
  }
}
