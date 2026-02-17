package com.kitten.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_participants", uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "seat_index"}))
@IdClass(GameParticipantId.class)
public class GameParticipant {

  @Id
  @Column(name = "game_id", columnDefinition = "uuid")
  private UUID gameId;

  @Id
  @Column(name = "user_id", columnDefinition = "uuid")
  private UUID userId;

  @Column(name = "seat_index", nullable = false)
  private int seatIndex;

  @Column(name = "result")
  private String result; // WIN, ELIMINATED

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id", insertable = false, updatable = false)
  private Game game;

  public GameParticipant() {}

  public GameParticipant(UUID gameId, UUID userId, int seatIndex) {
    this.gameId = gameId;
    this.userId = userId;
    this.seatIndex = seatIndex;
    this.createdAt = Instant.now();
  }

  public UUID getGameId() {
    return gameId;
  }

  public void setGameId(UUID gameId) {
    this.gameId = gameId;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public int getSeatIndex() {
    return seatIndex;
  }

  public void setSeatIndex(int seatIndex) {
    this.seatIndex = seatIndex;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
