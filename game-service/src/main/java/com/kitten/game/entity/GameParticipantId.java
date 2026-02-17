package com.kitten.game.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class GameParticipantId implements Serializable {

  private UUID gameId;
  private UUID userId;

  public GameParticipantId() {}

  public GameParticipantId(UUID gameId, UUID userId) {
    this.gameId = gameId;
    this.userId = userId;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GameParticipantId that = (GameParticipantId) o;
    return Objects.equals(gameId, that.gameId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gameId, userId);
  }
}
