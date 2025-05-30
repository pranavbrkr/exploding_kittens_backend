package com.kitten.game.model;

import java.util.List;

public class PlayerState {
  private String playerId;
  private List<CardType> hand;

  public PlayerState() {}

  public PlayerState(String playerId, List<CardType> hand) {
    this.playerId = playerId;
    this.hand = hand;
  }

  public String getPlayerId() {
    return playerId;
  }

  public void setPlayer(String playerId) {
    this.playerId = playerId;
  }

  public List<CardType> getHand() {
    return hand;
  }

  public void setHand(List<CardType> hand) {
    this.hand = hand;
  }
}
