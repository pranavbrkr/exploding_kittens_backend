package com.kitten.game.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kitten.game.model.CardType;
import com.kitten.game.model.GameState;
import com.kitten.game.model.PlayerState;

@Service
public class GameService {
  
  private final Map<String, GameState> gameStore = new HashMap<>();
  
  public GameState startGame(String lobbyId, List<String> playerIds) {
    if (gameStore.containsKey(lobbyId)) {
      return gameStore.get(lobbyId);
    }

    List<CardType> deck = new ArrayList<>();
    deck.addAll(Collections.nCopies(3, CardType.DEFUSE));
    deck.addAll(Collections.nCopies(3, CardType.ATTACK));
    deck.addAll(Collections.nCopies(3, CardType.TARGETED_ATTACK));
    deck.addAll(Collections.nCopies(6, CardType.SKIP));
    deck.addAll(Collections.nCopies(3, CardType.SEE_THE_FUTURE));
    deck.addAll(Collections.nCopies(4, CardType.ALTER_THE_FUTURE));
    deck.addAll(Collections.nCopies(4, CardType.SHUFFLE));
    deck.addAll(Collections.nCopies(4, CardType.DRAW_FROM_BOTTOM));
    deck.addAll(Collections.nCopies(4, CardType.FAVOR));
    deck.addAll(Collections.nCopies(5, CardType.NOPE));
    deck.addAll(Collections.nCopies(4, CardType.CAT_TACO));
    deck.addAll(Collections.nCopies(4, CardType.CAT_WATERMELON));
    deck.addAll(Collections.nCopies(4, CardType.CAT_POTATO));
    deck.addAll(Collections.nCopies(4, CardType.CAT_BEARD));
    deck.addAll(Collections.nCopies(4, CardType.CAT_RAINBOW));
    deck.addAll(Collections.nCopies(4, CardType.CAT_FERAL));

    Collections.shuffle(deck);

    List<PlayerState> players = new ArrayList<>();
    for (String id: playerIds) {
      List<CardType> hand = new ArrayList<>();
      hand.add(CardType.DEFUSE);
      for (int i = 0; i < 7; i++) {
        hand.add(deck.remove(0));
      }
      players.add(new PlayerState(id, hand));
    }

    deck.addAll(Collections.nCopies(3, CardType.EXPLODING_KITTEN));
    Collections.shuffle(deck);
    Collections.shuffle(deck);
    Collections.shuffle(deck);

    GameState game = new GameState();
    game.setLobbyId(lobbyId);
    game.setPlayers(players);
    game.setDeck(deck);
    // game.setUsedCards(new ArrayList<>());
    game.setCardsToDraw(1);
    game.setGameStarted(true);
    game.setCurrentPlayerIndex(0);

    gameStore.put(lobbyId, game);
    return game;
  }

  public GameState getGame(String lobbyId) {
    return gameStore.get(lobbyId);
  }

}
