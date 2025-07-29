package com.kitten.game.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.kitten.game.model.CardType;
import com.kitten.game.model.GameState;
import com.kitten.game.model.PlayerState;

@Service
public class GameService {
  
  private final Map<String, GameState> gameStore = new HashMap<>();
  
  /**
   * Finds the next valid player index, skipping eliminated players
   */
  private int findNextValidPlayerIndex(GameState game, int currentIndex) {
    if (game.getPlayers().isEmpty()) {
      return -1; // No players left
    }
    
    // Since eliminated players are removed from the list, we just need to go to the next index
    // If we're at the end, wrap around to the beginning
    int nextIndex = (currentIndex + 1) % game.getPlayers().size();
    return nextIndex;
  }
  
  /**
   * Finds the first valid player index
   */
  private int findFirstValidPlayerIndex(GameState game) {
    if (game.getPlayers().isEmpty()) {
      return -1;
    }
    return 0; // Since eliminated players are removed from the list, index 0 should be valid
  }
  
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
    // deck.addAll(Collections.nCopies(5, CardType.NOPE));
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

  /**
   * Checks if the game is over and returns the winner if applicable
   * @return winner player ID if game is over, null otherwise
   */
  public String getGameWinner(String lobbyId) {
    GameState game = gameStore.get(lobbyId);
    if (game == null) return null;
    
    // Game is over if only one player remains
    if (game.getPlayers().size() == 1) {
      return game.getPlayers().get(0).getPlayerId();
    }
    
    // Game is over if no players remain (shouldn't happen in normal gameplay)
    if (game.getPlayers().isEmpty()) {
      return null;
    }
    
    return null; // Game is still ongoing
  }

  public boolean handleDrawnCard(CardType drawnCard, PlayerState player, GameState game) {
    if (drawnCard == CardType.EXPLODING_KITTEN) {
      game.setCardsToDraw(game.getCardsToDraw() - 1);

      if (player.getHand().remove(CardType.DEFUSE)) {
        game.getUsedCards().add(CardType.DEFUSE);
        int pos = new Random().nextInt(game.getDeck().size() + 1);
        game.getDeck().add(pos, CardType.EXPLODING_KITTEN);
        game.setCardsToDraw(0);
        
        // After using Defuse, turn should pass to the next player
        int currentIndex = game.getCurrentPlayerIndex();
        int nextIndex = findNextValidPlayerIndex(game, currentIndex);
        game.setCurrentPlayerIndex(nextIndex);
        
        return true;
      } else {
        // Player is eliminated
        // Store the current player index before removing the player
        int eliminatedPlayerIndex = game.getCurrentPlayerIndex();
        
        game.getPlayers().remove(player);
        game.getEliminatedPlayers().add(player.getPlayerId());
        
        // Check if game is over (only one player left)
        if (game.getPlayers().size() <= 1) {
          // Game is over - winner is the remaining player
          game.setCardsToDraw(0);
          return true;
        }
        
        // After removing the player, the indices have shifted
        // The next player in the original sequence is now at the same index as the eliminated player
        // (since everything after the eliminated player shifted down by 1)
        int nextIndex;
        if (eliminatedPlayerIndex >= game.getPlayers().size()) {
          // Eliminated player was at the end, so go to the first player
          nextIndex = 0;
        } else {
          // The next player in sequence is now at the same index as the eliminated player
          nextIndex = eliminatedPlayerIndex;
        }
        
        game.setCurrentPlayerIndex(nextIndex);
        game.setCardsToDraw(0);
        return true;
      }
    } else {
      player.getHand().add(drawnCard);
      game.setCardsToDraw(game.getCardsToDraw() - 1);
    }

    return false;
  }

}
