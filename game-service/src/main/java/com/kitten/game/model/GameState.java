package com.kitten.game.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
  private String lobbyId;
  private List<PlayerState> players;
  private List<String> eliminatedPlayers = new ArrayList<>();
  private List<CardType> deck;
  private List<CardType> usedCards = new ArrayList<>();
  private int currentPlayerIndex;
  private int cardsToDraw;
  private boolean gameStarted;
  private String favorFromPlayerId;
  private String targetedAttackTargetId;

  public GameState() {}

  public GameState(String lobbyId, List<PlayerState> players, List<CardType> deck, int currentPlayerIndex, boolean gameStarted) {
    this.lobbyId = lobbyId;
    this.players = players;
    this.deck = deck;
    this.currentPlayerIndex = currentPlayerIndex;
    this.gameStarted = gameStarted;
  }

  public String getLobbyId() {
    return lobbyId;
  }

  public void setLobbyId(String lobbyId) {
    this.lobbyId = lobbyId;
  }

  public List<PlayerState> getPlayers() {
    return players;
  }

  public void setPlayers(List<PlayerState> players) {
    this.players = players;
  }

  public List<String> getEliminatedPlayers() {
    return eliminatedPlayers;
  }

  public List<CardType> getDeck() {
    return deck;
  }

  public void setDeck(List<CardType> deck) {
    this.deck = deck;
  }

  public List<CardType> getUsedCards() {
    return usedCards;
  }

  public void setUsedCards(List<CardType> usedCards) {
    this.usedCards = usedCards;
  }

  public int getCurrentPlayerIndex() {
    return currentPlayerIndex;
  }

  public void setCurrentPlayerIndex(int currentPlayerIndex) {
    this.currentPlayerIndex = currentPlayerIndex;
  }

  public int getCardsToDraw() {
    return cardsToDraw;
  }

  public void setCardsToDraw(int cardsToDraw) {
    this.cardsToDraw = cardsToDraw;
  }

  public boolean isGameStarted() {
    return gameStarted;
  }

  public void setGameStarted(boolean gameStarted) {
    this.gameStarted = gameStarted;
  }

  public String getFavorFromPlayerId() {
    return favorFromPlayerId;
  }

  public void setFavorFromPlayerId(String favorFromPlayerId) {
    this.favorFromPlayerId = favorFromPlayerId;
  }

  public String getTargetedAttackTargetId() {
    return targetedAttackTargetId;
  }

  public void setTargetedAttackTargetId(String targetedAttackTargetId) {
    this.targetedAttackTargetId = targetedAttackTargetId;
  }
}
