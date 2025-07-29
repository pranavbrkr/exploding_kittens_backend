package com.kitten.game.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kitten.game.model.CardType;
import com.kitten.game.model.GameState;
import com.kitten.game.model.PlayerState;
import com.kitten.game.service.GameService;

@RestController
@RequestMapping("/game")
public class GameController {

  @Autowired
  private GameService gameService;

  @Autowired
  private SimpMessagingTemplate messagingTemplate;

  /**
   * Finds the next valid player index
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

  @PostMapping("/start")
  public GameState startGame(@RequestParam("lobbyId") String lobbyId, @RequestBody List<String> playerIds) {
    GameState game =  gameService.startGame(lobbyId, playerIds);

    String currentPlayerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getPlayerId();
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", currentPlayerId);

    return game;
  }

  @GetMapping("/{lobbyId}")
  public ResponseEntity<GameState> getGameState(@PathVariable("lobbyId") String lobbyId) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(game);
  }

  @GetMapping("/{lobbyId}/winner")
  public ResponseEntity<String> getGameWinner(@PathVariable("lobbyId") String lobbyId) {
    String winner = gameService.getGameWinner(lobbyId);
    if (winner == null) {
      return ResponseEntity.ok(""); // Game is still ongoing
    }
    return ResponseEntity.ok(winner);
  }

  @PostMapping("/skip/{lobbyId}")
  public ResponseEntity<Void> skipTurn(@PathVariable String lobbyId) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    int next = findNextValidPlayerIndex(game, game.getCurrentPlayerIndex());
    if (next == -1) return ResponseEntity.badRequest().build(); // No players left
    
    game.setCurrentPlayerIndex(next);

    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", game.getPlayers().get(next).getPlayerId());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/play/{lobbyId}")
  public ResponseEntity<Void> playCard(@PathVariable String lobbyId, @RequestParam String playerId, @RequestParam String cardType) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();
    boolean endTurnEarly = false;

    int currentIndex = game.getCurrentPlayerIndex();
    PlayerState currentPlayer = game.getPlayers().get(currentIndex);
    if (!currentPlayer.getPlayerId().equals(playerId)) return ResponseEntity.status(403).build();

    CardType card = CardType.valueOf(cardType);
    if (!currentPlayer.getHand().remove(card)) return ResponseEntity.badRequest().build();

    game.getUsedCards().add(card);

    if (card == CardType.SHUFFLE) {
      Collections.shuffle(game.getDeck());
      return ResponseEntity.ok().build();
    }

    if (card == CardType.SKIP) {
      game.setCardsToDraw(game.getCardsToDraw() - 1);
    }

    if (card == CardType.SEE_THE_FUTURE) {
      int end = Math.min(3, game.getDeck().size());
      List<CardType> topCards = game.getDeck().subList(0, end);
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/future/" + playerId, topCards);
      return ResponseEntity.ok().build();
    }

    if (card == CardType.ALTER_THE_FUTURE) {
      int end = Math.min(3, game.getDeck().size());
      List<CardType> topCards = game.getDeck().subList(0, end);
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/alter/" + playerId, topCards);
      return ResponseEntity.ok().build();
    }

    if (card == CardType.DRAW_FROM_BOTTOM) {
      if (!game.getDeck().isEmpty()) {
        CardType drawnCard = game.getDeck().remove(game.getDeck().size() - 1);
        endTurnEarly = gameService.handleDrawnCard(drawnCard, currentPlayer, game);
        // currentPlayer.getHand().add(game.getDeck().remove(game.getDeck().size() - 1));
        // game.setCardsToDraw(game.getCardsToDraw() - 1);
      }
    }

    if (card == CardType.FAVOR) {
      game.setFavorFromPlayerId(playerId); // Set who played Favor
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/favor/select/" + playerId, game.getPlayers().stream()
        .map(PlayerState::getPlayerId)
        .filter(pid -> !pid.equals(playerId))
        .toList());
      return ResponseEntity.ok().build();
    }

    if (card == CardType.ATTACK) {
      int next = findNextValidPlayerIndex(game, currentIndex);
      if (next == -1) return ResponseEntity.badRequest().build(); // No players left
      
      game.setCurrentPlayerIndex(next);
      game.setCardsToDraw(game.getCardsToDraw() + 2); // 2 from attack + 1 normal = 3

      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", game.getPlayers().get(next).getPlayerId());
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");

      return ResponseEntity.ok().build();
    }

    if (card == CardType.TARGETED_ATTACK) {
      List<String> targets = game.getPlayers().stream()
          .map(PlayerState::getPlayerId)
          .filter(pid -> !pid.equals(playerId))
          .toList();

      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/targeted/select/" + playerId, targets);
      return ResponseEntity.ok().build();
    }

    if (endTurnEarly || game.getCardsToDraw() <= 0) {
      int next = findNextValidPlayerIndex(game, currentIndex);
      if (next == -1) return ResponseEntity.badRequest().build(); // No players left
      
      game.setCurrentPlayerIndex(next);
      game.setCardsToDraw(1);

      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", game.getPlayers().get(next).getPlayerId());
    }

    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", game);

    return ResponseEntity.ok().build();
  }

  @PostMapping("/targeted/confirm/{lobbyId}")
  public ResponseEntity<Void> confirmTargetedAttack(
      @PathVariable String lobbyId,
      @RequestParam String fromPlayerId,
      @RequestParam String toPlayerId) {
      
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    game.setTargetedAttackTargetId(toPlayerId);
    game.setCardsToDraw(game.getCardsToDraw() + 2); // 2 attack + 1 normal

    // Update turn to the target
    int targetIndex = -1;
    for (int i = 0; i < game.getPlayers().size(); i++) {
      if (game.getPlayers().get(i).getPlayerId().equals(toPlayerId)) {
        targetIndex = i;
        break;
      }
    }

    if (targetIndex == -1) return ResponseEntity.badRequest().build();
    game.setCurrentPlayerIndex(targetIndex);

    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", toPlayerId);
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");

    return ResponseEntity.ok().build();
  }

  @PostMapping("/favor/response/{lobbyId}")
  public ResponseEntity<Void> handleFavorResponse(@PathVariable String lobbyId, @RequestParam String fromPlayerId, @RequestParam String toPlayerId, @RequestParam String givenCard) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    CardType card = CardType.valueOf(givenCard);

    PlayerState fromPlayer = game.getPlayers().stream()
      .filter(p -> p.getPlayerId().equals(fromPlayerId)).findFirst().orElse(null);
    PlayerState toPlayer = game.getPlayers().stream()
      .filter(p -> p.getPlayerId().equals(toPlayerId)).findFirst().orElse(null);

    if (fromPlayer == null || toPlayer == null || !fromPlayer.getHand().remove(card)) {
      return ResponseEntity.badRequest().build();
    }

    toPlayer.getHand().add(card);
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");
    return ResponseEntity.ok().build();
  }


  @PostMapping("/favor/request/{lobbyId}")
  public ResponseEntity<Void> favorRequest(@PathVariable String lobbyId, @RequestParam String fromPlayerId, @RequestParam String toPlayerId) {
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/favor/request/" + toPlayerId, fromPlayerId);
    return ResponseEntity.ok().build();
  }


  @PostMapping("/cat-combo/{lobbyId}")
  public ResponseEntity<Void> handleCatCombo(@PathVariable String lobbyId, @RequestParam String playerId, @RequestBody List<CardType> cats) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null || cats.size() != 2 || !cats.get(0).equals(cats.get(1))) return ResponseEntity.badRequest().build();

    PlayerState player = game.getPlayers().stream()
        .filter(p -> p.getPlayerId().equals(playerId)).findFirst().orElse(null);
    if (player == null) return ResponseEntity.notFound().build();

    // Remove cat cards from hand
    if (!player.getHand().remove(cats.get(0)) || !player.getHand().remove(cats.get(1))) {
        return ResponseEntity.badRequest().build();
    }

    game.setSelectedCatCards(cats);

    // Notify frontend to select opponent
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/cat/select-opponent/" + playerId,
        game.getPlayers().stream()
            .map(PlayerState::getPlayerId)
            .filter(pid -> !pid.equals(playerId))
            .toList());

    return ResponseEntity.ok().build();
  }

  @GetMapping("/cat/opponents/{lobbyId}")
  public ResponseEntity<List<String>> getValidStealTargets(@PathVariable String lobbyId, @RequestParam String playerId) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    List<String> targets = game.getPlayers().stream()
        .filter(p -> !p.getPlayerId().equals(playerId)
            && !game.getEliminatedPlayers().contains(p.getPlayerId()))
        .map(PlayerState::getPlayerId)
        .collect(Collectors.toList());

    return ResponseEntity.ok(targets);
  }


  @PostMapping("/cat/steal/{lobbyId}")
  public ResponseEntity<Void> handleCatStealTarget(@PathVariable String lobbyId, @RequestParam String fromPlayerId, @RequestParam String toPlayerId) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    PlayerState target = game.getPlayers().stream()
        .filter(p -> p.getPlayerId().equals(toPlayerId)).findFirst().orElse(null);

    if (target == null || target.getHand().isEmpty()) {
        return ResponseEntity.badRequest().build();
    }

    List<CardType> shuffledHand = new ArrayList<>(target.getHand());
    Collections.shuffle(shuffledHand);

    List<Integer> indices = new ArrayList<>();
    for (int i = 1; i <= shuffledHand.size(); i++) {
        indices.add(i); // Show numbered options
    }

    // Send to frontend with count of options (1..N)
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/cat/select-number/" + fromPlayerId,
        indices);

    // Store mapping for future lookup
    game.setPendingStealFromPlayerId(toPlayerId);
    game.setSelectedCatCards(shuffledHand); // reuse to store randomized view

    return ResponseEntity.ok().build();
  }


  @PostMapping("/cat/steal/resolve/{lobbyId}")
  public ResponseEntity<Void> resolveSteal(@PathVariable String lobbyId, @RequestParam String stealerId, @RequestParam int selectedIndex) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    String targetId = game.getPendingStealFromPlayerId();
    PlayerState from = game.getPlayers().stream()
        .filter(p -> p.getPlayerId().equals(stealerId)).findFirst().orElse(null);
    PlayerState to = game.getPlayers().stream()
        .filter(p -> p.getPlayerId().equals(targetId)).findFirst().orElse(null);

    if (from == null || to == null || selectedIndex < 1 || selectedIndex > game.getSelectedCatCards().size()) {
        return ResponseEntity.badRequest().build();
    }

    CardType stolen = game.getSelectedCatCards().get(selectedIndex - 1);

    if (!to.getHand().remove(stolen)) return ResponseEntity.badRequest().build();

    // Remove 2 identical cat cards from the stealer's hand
    Map<CardType, Long> catCardCounts = from.getHand().stream()
        .filter(c -> c.name().startsWith("CAT_"))
        .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

    CardType catUsed = null;
    for (Map.Entry<CardType, Long> entry : catCardCounts.entrySet()) {
        if (entry.getValue() >= 2) {
            catUsed = entry.getKey();
            break;
        }
    }

    if (catUsed == null) {
        return ResponseEntity.badRequest().build(); // sanity check
    }

    // Remove two instances of the used cat card
    int removed = 0;
    List<CardType> newHand = new ArrayList<>();
    for (CardType c : from.getHand()) {
        if (removed < 2 && c == catUsed) {
            removed++;
            continue;
        }
        newHand.add(c);
    }
    from.setHand(newHand);
    // Add both used cat cards to the used pile
    game.getUsedCards().add(catUsed);
    game.getUsedCards().add(catUsed);

    from.getHand().add(stolen);

    // Clear temp state
    game.setSelectedCatCards(new ArrayList<>());
    game.setPendingStealFromPlayerId(null);

    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");

    return ResponseEntity.ok().build();
  }


  @PostMapping("/draw/{lobbyId}")
  public ResponseEntity<Void> drawCard(@PathVariable String lobbyId, @RequestParam String playerId) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();
    boolean endTurnEarly = false;

    int currentIndex = game.getCurrentPlayerIndex();
    PlayerState currentPlayer = game.getPlayers().get(currentIndex);

    if(!currentPlayer.getPlayerId().equals(playerId)) {
      return ResponseEntity.status(403).build();
    }

    if (!game.getDeck().isEmpty()) {
      CardType drawnCard = game.getDeck().remove(0);
      endTurnEarly = gameService.handleDrawnCard(drawnCard, currentPlayer, game);
      // currentPlayer.getHand().add(game.getDeck().remove(0));
      // game.setCardsToDraw(game.getCardsToDraw() - 1);
    }

    if (endTurnEarly || game.getCardsToDraw() <= 0) {
      // If endTurnEarly is true, the GameService has already set the correct next player
      // We only need to handle normal turn progression
      if (!endTurnEarly) {
        int next = findNextValidPlayerIndex(game, currentIndex);
        if (next == -1) return ResponseEntity.badRequest().build(); // No players left
        
        game.setCurrentPlayerIndex(next);
        game.setCardsToDraw(1);

        messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", game.getPlayers().get(next).getPlayerId());
      } else {
        // Turn was already set by GameService, just reset cards to draw
        game.setCardsToDraw(1);
        
        // Send the turn update with the current player
        String currentPlayerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getPlayerId();
        messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", currentPlayerId);
      }
    }

    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");

    return ResponseEntity.ok().build();
  }

  @PostMapping("/alter/{lobbyId}")
  public ResponseEntity<Void> reorderFuture(@PathVariable String lobbyId, @RequestBody List<CardType> reorderedCards, @RequestParam String playerId) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    List<CardType> deck = game.getDeck();
    for (int i = 0; i < reorderedCards.size(); i++) {
      deck.set(i, reorderedCards.get(i));
    }

    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");
    return ResponseEntity.ok().build();
  }

  @PostMapping("/cat/steal-defuse/{lobbyId}")
  public ResponseEntity<Void> handleDefuseSteal( @PathVariable String lobbyId, @RequestParam String fromPlayerId, @RequestParam String toPlayerId) {

    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    PlayerState from = game.getPlayers().stream()
        .filter(p -> p.getPlayerId().equals(fromPlayerId)).findFirst().orElse(null);
    PlayerState to = game.getPlayers().stream()
        .filter(p -> p.getPlayerId().equals(toPlayerId)).findFirst().orElse(null);

    if (from == null || to == null) return ResponseEntity.badRequest().build();

    // Find 3 identical cat cards in "from"'s hand
    Map<CardType, Long> catCounts = from.getHand().stream()
        .filter(c -> c.name().startsWith("CAT_"))
        .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

    CardType catUsed = null;
    for (Map.Entry<CardType, Long> entry : catCounts.entrySet()) {
        if (entry.getValue() >= 3) {
            catUsed = entry.getKey();
            break;
        }
    }

    if (catUsed == null) return ResponseEntity.badRequest().build();

    // Remove 3 cat cards
    int removed = 0;
    List<CardType> newHand = new ArrayList<>();
    for (CardType c : from.getHand()) {
        if (removed < 3 && c == catUsed) {
            removed++;
            game.getUsedCards().add(c);
            continue;
        }
        newHand.add(c);
    }
    from.setHand(newHand);

    // Attempt to steal DEFUSE
    if (to.getHand().remove(CardType.DEFUSE)) {
        from.getHand().add(CardType.DEFUSE);
    }

    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");

    return ResponseEntity.ok().build();
  }

}
