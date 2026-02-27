package com.kitten.game.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class GameParticipantId implements Serializable {

  private UUID gameId;
  private String playerId;

  public GameParticipantId() {}

  public GameParticipantId(UUID gameId, String playerId) {
    this.gameId = gameId;
    this.playerId = playerId;
  }

  public UUID getGameId() {
    return gameId;
  }

  public void setGameId(UUID gameId) {
    this.gameId = gameId;
  }

  public String getPlayerId() {
    return playerId;
  }

  public void setPlayerId(String playerId) {
    this.playerId = playerId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GameParticipantId that = (GameParticipantId) o;
    return Objects.equals(gameId, that.gameId) && Objects.equals(playerId, that.playerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gameId, playerId);
  }
}
