package com.kitten.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_actions", uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "seq"}))
public class GameAction {

  @Id
  @Column(columnDefinition = "uuid")
  private UUID id;

  @Column(name = "game_id", nullable = false, columnDefinition = "uuid")
  private UUID gameId;

  @Column(name = "seq", nullable = false)
  private int seq;

  @Column(name = "turn_number")
  private Integer turnNumber;

  @Column(name = "actor_user_id", columnDefinition = "uuid")
  private UUID actorUserId;

  @Column(name = "receiver_user_id", columnDefinition = "uuid")
  private UUID receiverUserId;

  @Column(name = "action_type", nullable = false)
  private String actionType;

  @Column(name = "payload", columnDefinition = "text")
  private String payload;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public GameAction() {}

  public GameAction(UUID id, UUID gameId, int seq, String actionType) {
    this.id = id;
    this.gameId = gameId;
    this.seq = seq;
    this.actionType = actionType;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getGameId() {
    return gameId;
  }

  public void setGameId(UUID gameId) {
    this.gameId = gameId;
  }

  public int getSeq() {
    return seq;
  }

  public void setSeq(int seq) {
    this.seq = seq;
  }

  public Integer getTurnNumber() {
    return turnNumber;
  }

  public void setTurnNumber(Integer turnNumber) {
    this.turnNumber = turnNumber;
  }

  public UUID getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(UUID actorUserId) {
    this.actorUserId = actorUserId;
  }

  public UUID getReceiverUserId() {
    return receiverUserId;
  }

  public void setReceiverUserId(UUID receiverUserId) {
    this.receiverUserId = receiverUserId;
  }

  public String getActionType() {
    return actionType;
  }

  public void setActionType(String actionType) {
    this.actionType = actionType;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
