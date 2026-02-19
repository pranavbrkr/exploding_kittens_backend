package com.kitten.game.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kitten.game.entity.Game;
import com.kitten.game.entity.GameParticipant;
import com.kitten.game.model.CardType;
import com.kitten.game.model.GameState;
import com.kitten.game.model.PlayerState;
import com.kitten.game.repository.GameParticipantRepository;
import com.kitten.game.repository.GameRepository;

@Service
public class GameService {

  private final Map<String, GameState> gameStore = new HashMap<>();
  private final GameRepository gameRepository;
  private final GameParticipantRepository gameParticipantRepository;
  private final GameActionService gameActionService;

  public GameService(GameRepository gameRepository, GameParticipantRepository gameParticipantRepository, GameActionService gameActionService) {
    this.gameRepository = gameRepository;
    this.gameParticipantRepository = gameParticipantRepository;
    this.gameActionService = gameActionService;
  }
  
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
  
  public GameState startGame(String lobbyId, List<String> playerIds, List<String> playerNames) {
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
    for (int i = 0; i < playerIds.size(); i++) {
      String playerId = playerIds.get(i);
      String playerName = i < playerNames.size() ? playerNames.get(i) : "Player " + playerId;
      
      List<CardType> hand = new ArrayList<>();
      hand.add(CardType.DEFUSE);
      for (int j = 0; j < 7; j++) {
        hand.add(deck.remove(0));
      }
      players.add(new PlayerState(playerId, playerName, hand));
    }

    deck.addAll(Collections.nCopies(3, CardType.EXPLODING_KITTEN));
    Collections.shuffle(deck);
    Collections.shuffle(deck);
    Collections.shuffle(deck);

    GameState game = new GameState();
    game.setLobbyId(lobbyId);
    game.setPlayers(players);
    game.setDeck(deck);
    game.setCardsToDraw(1);
    game.setGameStarted(true);
    game.setCurrentPlayerIndex(0);

    // Persist game and participants
    UUID gameId = persistGameStart(lobbyId, playerIds);
    game.setGameId(gameId != null ? gameId.toString() : null);

    gameStore.put(lobbyId, game);
    return game;
  }

  @Transactional
  protected UUID persistGameStart(String lobbyId, List<String> playerIds) {
    try {
      UUID gameId = UUID.randomUUID();
      Game gameEntity = new Game(gameId, lobbyId);
      gameRepository.save(gameEntity);
      for (int i = 0; i < playerIds.size(); i++) {
        UUID userId = UUID.fromString(playerIds.get(i));
        gameParticipantRepository.save(new GameParticipant(gameId, userId, i));
      }
      return gameId;
    } catch (Exception e) {
      // Don't fail in-memory game if DB fails (e.g. game-service run without DB)
      org.slf4j.LoggerFactory.getLogger(GameService.class).warn("Failed to persist game start: {}", e.getMessage(), e);
      return null;
    }
  }

  @Transactional
  protected void persistGameFinished(String gameIdStr, String winnerPlayerId, List<String> eliminatedPlayerIds) {
    if (gameIdStr == null) return;
    try {
      UUID gameId = UUID.fromString(gameIdStr);
      Game game = gameRepository.findById(gameId).orElse(null);
      if (game == null || game.getStatus() == Game.GameStatus.FINISHED) return;
      game.setStatus(Game.GameStatus.FINISHED);
      game.setEndedAt(java.time.Instant.now());
      game.setWinnerUserId(UUID.fromString(winnerPlayerId));
      gameRepository.save(game);
      List<GameParticipant> participants = gameParticipantRepository.findByGameIdOrderBySeatIndex(gameId);
      for (GameParticipant p : participants) {
        if (p.getUserId().toString().equals(winnerPlayerId)) {
          p.setResult("WIN");
        } else if (eliminatedPlayerIds != null && eliminatedPlayerIds.contains(p.getUserId().toString())) {
          p.setResult("ELIMINATED");
        } else {
          p.setResult("LOSS");
        }
        gameParticipantRepository.save(p);
      }
    } catch (Exception e) {
      // Log but don't fail game flow
    }
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
        int eliminatedPlayerIndex = game.getCurrentPlayerIndex();
        String eliminatedPlayerId = player.getPlayerId();

        game.getPlayers().remove(player);
        game.getEliminatedPlayers().add(eliminatedPlayerId);
        gameActionService.recordActionWithPlayerIds(game.getGameId(), null, eliminatedPlayerId, null, "ELIMINATED", null);

        // Check if game is over (only one player left)
        if (game.getPlayers().size() <= 1) {
          game.setCardsToDraw(0);
          if (game.getPlayers().size() == 1) {
            game.setCurrentPlayerIndex(0); // winner is at index 0
            String winnerId = game.getPlayers().get(0).getPlayerId();
            persistGameFinished(game.getGameId(), winnerId, game.getEliminatedPlayers());
          }
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
