package com.kitten.game.model;

import java.util.List;

public class PlayerState {
  private String playerId;
  private String playerName;
  private List<CardType> hand;

  public PlayerState() {}

  public PlayerState(String playerId, List<CardType> hand) {
    this.playerId = playerId;
    this.hand = hand;
  }

  public PlayerState(String playerId, String playerName, List<CardType> hand) {
    this.playerId = playerId;
    this.playerName = playerName;
    this.hand = hand;
  }

  public String getPlayerId() {
    return playerId;
  }

  public void setPlayer(String playerId) {
    this.playerId = playerId;
  }

  public String getPlayerName() {
    return playerName;
  }

  public void setPlayerName(String playerName) {
    this.playerName = playerName;
  }

  public List<CardType> getHand() {
    return hand;
  }

  public void setHand(List<CardType> hand) {
    this.hand = hand;
  }
}
