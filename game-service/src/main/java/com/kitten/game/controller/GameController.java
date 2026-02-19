package com.kitten.game.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import com.kitten.game.service.GameActionService;
import com.kitten.game.service.GameService;

@RestController
@RequestMapping("/api/game")
public class GameController {

  @Autowired
  private GameService gameService;

  @Autowired
  private GameActionService gameActionService;

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
  public GameState startGame(@RequestParam("lobbyId") String lobbyId, @RequestBody GameStartRequest request) {
    GameState game = gameService.startGame(lobbyId, request.getPlayerIds(), request.getPlayerNames());

    gameActionService.recordActionWithPlayerIds(game.getGameId(), null, null, null, "GAME_START", null);

    String currentPlayerId = game.getPlayers().get(game.getCurrentPlayerIndex()).getPlayerId();
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", currentPlayerId);

    return game;
  }

  // Inner class for request
  public static class GameStartRequest {
    private List<String> playerIds;
    private List<String> playerNames;

    public List<String> getPlayerIds() { return playerIds; }
    public void setPlayerIds(List<String> playerIds) { this.playerIds = playerIds; }
    public List<String> getPlayerNames() { return playerNames; }
    public void setPlayerNames(List<String> playerNames) { this.playerNames = playerNames; }
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
    if (game.getPlayers().size() <= 1) return ResponseEntity.status(409).build(); // Game over

    int currentIndex = game.getCurrentPlayerIndex();
    if (currentIndex >= game.getPlayers().size()) return ResponseEntity.status(409).build();
    String currentPlayerId = game.getPlayers().get(currentIndex).getPlayerId();
    String currentPlayerName = game.getPlayers().get(currentIndex).getPlayerName();
    
    int next = findNextValidPlayerIndex(game, currentIndex);
    if (next == -1) return ResponseEntity.badRequest().build(); // No players left
    
    game.setCurrentPlayerIndex(next);

    // Send action notification
    Map<String, Object> actionData = new HashMap<>();
    actionData.put("message", currentPlayerName + " used SKIP");
    actionData.put("type", "info");
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);

    gameActionService.recordActionWithPlayerIds(game.getGameId(), null, currentPlayerId, null, "PLAY_SKIP", "{\"cardType\":\"SKIP\"}");
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", game.getPlayers().get(next).getPlayerId());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/play/{lobbyId}")
  public ResponseEntity<Void> playCard(@PathVariable String lobbyId, @RequestParam String playerId, @RequestParam String cardType) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();
    if (game.getPlayers().size() <= 1) return ResponseEntity.status(409).build(); // Game over
    boolean endTurnEarly = false;

    int currentIndex = game.getCurrentPlayerIndex();
    if (currentIndex >= game.getPlayers().size()) return ResponseEntity.status(409).build();
    PlayerState currentPlayer = game.getPlayers().get(currentIndex);
    if (!currentPlayer.getPlayerId().equals(playerId)) return ResponseEntity.status(403).build();

    CardType card = CardType.valueOf(cardType);
    if (!currentPlayer.getHand().remove(card)) return ResponseEntity.badRequest().build();

    game.getUsedCards().add(card);

    if (card == CardType.SHUFFLE) {
      gameActionService.recordActionWithPlayerIds(game.getGameId(), null, playerId, null, "PLAY_SHUFFLE", null);
      Collections.shuffle(game.getDeck());
      // Send action notification
      Map<String, Object> actionData = new HashMap<>();
      actionData.put("message", currentPlayer.getPlayerName() + " used SHUFFLE");
      actionData.put("type", "info");
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);
      
      return ResponseEntity.ok().build();
    }

    if (card == CardType.SKIP) {
      gameActionService.recordActionWithPlayerIds(game.getGameId(), null, playerId, null, "PLAY_SKIP", "{\"cardType\":\"SKIP\"}");
      game.setCardsToDraw(game.getCardsToDraw() - 1);
      // Send action notification
      Map<String, Object> actionData = new HashMap<>();
      actionData.put("message", currentPlayer.getPlayerName() + " used SKIP");
      actionData.put("type", "info");
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);
    }

    if (card == CardType.SEE_THE_FUTURE) {
      gameActionService.recordActionWithPlayerIds(game.getGameId(), null, playerId, null, "PLAY_SEE_THE_FUTURE", null);
      int end = Math.min(3, game.getDeck().size());
      List<CardType> topCards = game.getDeck().subList(0, end);
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/future/" + playerId, topCards);
      // Send action notification
      Map<String, Object> actionData = new HashMap<>();
      actionData.put("message", currentPlayer.getPlayerName() + " used SEE THE FUTURE");
      actionData.put("type", "info");
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);
      
      return ResponseEntity.ok().build();
    }

    if (card == CardType.ALTER_THE_FUTURE) {
      gameActionService.recordActionWithPlayerIds(game.getGameId(), null, playerId, null, "PLAY_ALTER_THE_FUTURE", null);
      int end = Math.min(3, game.getDeck().size());
      List<CardType> topCards = game.getDeck().subList(0, end);
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/alter/" + playerId, topCards);
      // Send action notification
      Map<String, Object> actionData = new HashMap<>();
      actionData.put("message", currentPlayer.getPlayerName() + " used ALTER THE FUTURE");
      actionData.put("type", "info");
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);
      
      return ResponseEntity.ok().build();
    }

    if (card == CardType.DRAW_FROM_BOTTOM) {
      if (!game.getDeck().isEmpty()) {
        CardType drawnCard = game.getDeck().remove(game.getDeck().size() - 1);
        endTurnEarly = gameService.handleDrawnCard(drawnCard, currentPlayer, game);
        gameActionService.recordActionWithPlayerIds(game.getGameId(), null, playerId, null, "PLAY_DRAW_FROM_BOTTOM", "{\"cardType\":\"" + drawnCard.name() + "\"}");
        // Send action notification
        Map<String, Object> actionData = new HashMap<>();
        actionData.put("message", currentPlayer.getPlayerName() + " used DRAW FROM BOTTOM");
        actionData.put("type", "info");
        messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);
        // currentPlayer.getHand().add(game.getDeck().remove(game.getDeck().size() - 1));
        // game.setCardsToDraw(game.getCardsToDraw() - 1);
      }
    }

    if (card == CardType.FAVOR) {
      game.setFavorFromPlayerId(playerId);
      gameActionService.recordActionWithPlayerIds(game.getGameId(), null, playerId, null, "PLAY_FAVOR", null);
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/favor/select/" + playerId, game.getPlayers().stream()
        .map(PlayerState::getPlayerId)
        .filter(pid -> !pid.equals(playerId))
        .toList());
      
      // Send action notification
      Map<String, Object> actionData = new HashMap<>();
      actionData.put("message", currentPlayer.getPlayerName() + " used FAVOR");
      actionData.put("type", "info");
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);
      
      return ResponseEntity.ok().build();
    }

    if (card == CardType.ATTACK) {
      int next = findNextValidPlayerIndex(game, currentIndex);
      if (next == -1) return ResponseEntity.badRequest().build();
      String nextPlayerId = game.getPlayers().get(next).getPlayerId();
      gameActionService.recordActionWithPlayerIds(game.getGameId(), null, playerId, nextPlayerId, "PLAY_ATTACK", null);

      game.setCurrentPlayerIndex(next);
      game.setCardsToDraw(game.getCardsToDraw() + 2);

      // Send action notification
      Map<String, Object> actionData = new HashMap<>();
      actionData.put("message", currentPlayer.getPlayerName() + " used ATTACK");
      actionData.put("type", "warning");
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);

      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", game.getPlayers().get(next).getPlayerId());
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");

      return ResponseEntity.ok().build();
    }

    if (card == CardType.TARGETED_ATTACK) {
      gameActionService.recordActionWithPlayerIds(game.getGameId(), null, playerId, null, "PLAY_TARGETED_ATTACK", null);
      List<String> targets = game.getPlayers().stream()
          .map(PlayerState::getPlayerId)
          .filter(pid -> !pid.equals(playerId))
          .toList();

      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/targeted/select/" + playerId, targets);
      
      // Send action notification
      Map<String, Object> actionData = new HashMap<>();
      actionData.put("message", currentPlayer.getPlayerName() + " used TARGETED ATTACK");
      actionData.put("type", "warning");
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);
      
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

    gameActionService.recordActionWithPlayerIds(game.getGameId(), null, fromPlayerId, toPlayerId, "TARGETED_ATTACK_CONFIRM", null);
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

    // Get player names for the notification
    String fromPlayerName = "Unknown Player";
    String toPlayerName = "Unknown Player";
    
    for (PlayerState player : game.getPlayers()) {
      if (player.getPlayerId().equals(fromPlayerId)) {
        fromPlayerName = player.getPlayerName();
      }
      if (player.getPlayerId().equals(toPlayerId)) {
        toPlayerName = player.getPlayerName();
      }
    }
    
    // Send action notification
    Map<String, Object> actionData = new HashMap<>();
    actionData.put("message", fromPlayerName + " used targeted attack on " + toPlayerName);
    actionData.put("type", "warning");
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);

    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", toPlayerId);
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");

    return ResponseEntity.ok().build();
  }

  @PostMapping("/favor/response/{lobbyId}")
  public ResponseEntity<Void> handleFavorResponse(@PathVariable String lobbyId, @RequestParam String fromPlayerId, @RequestParam String toPlayerId, @RequestParam String givenCard) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    gameActionService.recordActionWithPlayerIds(game.getGameId(), null, toPlayerId, fromPlayerId, "FAVOR_RESPONSE", "{\"givenCard\":\"" + givenCard + "\"}");

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
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    gameActionService.recordActionWithPlayerIds(game.getGameId(), null, fromPlayerId, toPlayerId, "FAVOR_REQUEST", null);

    // Get player names for the notification
    String fromPlayerName = "Unknown Player";
    String toPlayerName = "Unknown Player";
    
    for (PlayerState player : game.getPlayers()) {
      if (player.getPlayerId().equals(fromPlayerId)) {
        fromPlayerName = player.getPlayerName();
      }
      if (player.getPlayerId().equals(toPlayerId)) {
        toPlayerName = player.getPlayerName();
      }
    }
    
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/favor/request/" + toPlayerId, fromPlayerId);
    
    // Send action notification
    Map<String, Object> actionData = new HashMap<>();
    actionData.put("message", fromPlayerName + " asked favor from " + toPlayerName);
    actionData.put("type", "info");
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);
    
    return ResponseEntity.ok().build();
  }


  @PostMapping("/cat-combo/{lobbyId}")
  public ResponseEntity<Void> handleCatCombo(@PathVariable String lobbyId, @RequestParam String playerId, @RequestBody List<CardType> cats) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    PlayerState player = game.getPlayers().stream()
        .filter(p -> p.getPlayerId().equals(playerId)).findFirst().orElse(null);
    if (player == null) return ResponseEntity.notFound().build();

    // Validate cat combination
    CatComboResult comboResult = validateCatCombo(cats);
    if (!comboResult.isValid()) {
        return ResponseEntity.badRequest().build();
    }

    // Remove cat cards from hand
    for (CardType cat : cats) {
        if (!player.getHand().remove(cat)) {
            return ResponseEntity.badRequest().build();
        }
    }

    game.setSelectedCatCards(cats);
    game.setCatComboType(comboResult.getComboType());
    gameActionService.recordActionWithPlayerIds(game.getGameId(), null, playerId, null, "CAT_COMBO", "{\"comboType\":\"" + comboResult.getComboType() + "\"}");

    // Notify frontend to select opponent
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/cat/select-opponent/" + playerId,
        game.getPlayers().stream()
            .map(PlayerState::getPlayerId)
            .filter(pid -> !pid.equals(playerId))
            .toList());

    return ResponseEntity.ok().build();
  }

  // Helper class to store cat combo validation results
  private static class CatComboResult {
    private final boolean valid;
    private final String comboType; // "steal_random" or "steal_defuse"

    public CatComboResult(boolean valid, String comboType) {
      this.valid = valid;
      this.comboType = comboType;
    }

    public boolean isValid() { return valid; }
    public String getComboType() { return comboType; }
  }

  // Validate cat combinations including feral cats
  private CatComboResult validateCatCombo(List<CardType> cats) {
    if (cats == null || cats.size() < 2 || cats.size() > 3) {
      return new CatComboResult(false, null);
    }

    // Count feral cats and regular cats
    long feralCount = cats.stream().filter(c -> c == CardType.CAT_FERAL).count();
    long regularCatCount = cats.stream().filter(c -> c != CardType.CAT_FERAL && c.name().startsWith("CAT_")).count();
    
    // Check for valid combinations:
    
    // For steal random card (2 cards):
    
    // 1. 2 same cat cards (any cat cards, including feral)
    if (cats.size() == 2) {
      CardType firstCat = cats.get(0);
      if (firstCat == cats.get(1) && firstCat.name().startsWith("CAT_")) {
        return new CatComboResult(true, "steal_random");
      }
    }
    
    // 2. 1 feral + 1 regular cat = steal random card
    if (cats.size() == 2 && feralCount == 1 && regularCatCount == 1) {
      return new CatComboResult(true, "steal_random");
    }
    
    // For steal defuse (3 cards):
    
    // 3. 3 same cat cards (any cat cards, including feral)
    if (cats.size() == 3) {
      CardType firstCat = cats.get(0);
      if (firstCat == cats.get(1) && firstCat == cats.get(2) && firstCat.name().startsWith("CAT_")) {
        return new CatComboResult(true, "steal_defuse");
      }
    }
    
    // 4. 2 feral + 1 regular cat = steal defuse
    if (cats.size() == 3 && feralCount == 2 && regularCatCount == 1) {
      List<CardType> ferals = cats.stream().filter(c -> c == CardType.CAT_FERAL).collect(Collectors.toList());
      List<CardType> regulars = cats.stream().filter(c -> c != CardType.CAT_FERAL && c.name().startsWith("CAT_")).collect(Collectors.toList());
      
      if (ferals.size() == 2 && regulars.size() == 1) {
        return new CatComboResult(true, "steal_defuse");
      }
    }
    
    // 5. 1 feral + 2 same regular cat cards = steal defuse
    if (cats.size() == 3 && feralCount == 1 && regularCatCount == 2) {
      List<CardType> regulars = cats.stream().filter(c -> c != CardType.CAT_FERAL && c.name().startsWith("CAT_")).collect(Collectors.toList());
      if (regulars.size() == 2 && regulars.get(0) == regulars.get(1)) {
        return new CatComboResult(true, "steal_defuse");
      }
    }

    return new CatComboResult(false, null);
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

    String comboType = game.getCatComboType();
    if (comboType == null) {
        return ResponseEntity.badRequest().build();
    }

    PlayerState target = game.getPlayers().stream()
        .filter(p -> p.getPlayerId().equals(toPlayerId)).findFirst().orElse(null);

    if (target == null) {
        return ResponseEntity.badRequest().build();
    }

    gameActionService.recordActionWithPlayerIds(game.getGameId(), null, fromPlayerId, toPlayerId, "CAT_STEAL", "{\"comboType\":\"" + comboType + "\"}");

    if ("steal_defuse".equals(comboType)) {
        // Direct defuse stealing - no need to select card
        return handleDefuseStealDirect(lobbyId, fromPlayerId, toPlayerId);
    } else if ("steal_random".equals(comboType)) {
        // Random card stealing - need to select from opponent's hand
        if (target.getHand().isEmpty()) {
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

    return ResponseEntity.badRequest().build();
  }

  // Helper method for direct defuse stealing
  private ResponseEntity<Void> handleDefuseStealDirect(String lobbyId, String fromPlayerId, String toPlayerId) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    PlayerState from = game.getPlayers().stream()
        .filter(p -> p.getPlayerId().equals(fromPlayerId)).findFirst().orElse(null);
    PlayerState to = game.getPlayers().stream()
        .filter(p -> p.getPlayerId().equals(toPlayerId)).findFirst().orElse(null);

    if (from == null || to == null) return ResponseEntity.badRequest().build();

    // Get player names for the notification
    String stealerName = from.getPlayerName();
    String targetName = to.getPlayerName();
    
    // Attempt to steal DEFUSE
    boolean defuseStolen = false;
    if (to.getHand().remove(CardType.DEFUSE)) {
        from.getHand().add(CardType.DEFUSE);
        defuseStolen = true;
    }
    
    // Send action notification for defuse stealing
    Map<String, Object> actionData = new HashMap<>();
    if (defuseStolen) {
        actionData.put("message", stealerName + " stole a DEFUSE card from " + targetName + " using cat cards");
        actionData.put("type", "warning");
    } else {
        actionData.put("message", stealerName + " attempted to steal DEFUSE from " + targetName + " but failed");
        actionData.put("type", "info");
    }
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);

    // Clear the cat combo state
    game.setSelectedCatCards(new ArrayList<>());
    game.setCatComboType(null);
    game.setPendingStealFromPlayerId(null);

    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");

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

    // The cat cards were already removed when the combo was played
    // Just add them to the used pile
    for (CardType cat : game.getSelectedCatCards()) {
        game.getUsedCards().add(cat);
    }

    from.getHand().add(stolen);

    // Get player names for the notification
    String stealerName = from.getPlayerName();
    String targetName = to.getPlayerName();
    
    // Send action notification for cat card stealing
    Map<String, Object> actionData = new HashMap<>();
    actionData.put("message", stealerName + " stole a card from " + targetName + " using cat cards");
    actionData.put("type", "info");
    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);

    // Clear temp state
    game.setSelectedCatCards(new ArrayList<>());
    game.setPendingStealFromPlayerId(null);
    game.setCatComboType(null);

    messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");

    return ResponseEntity.ok().build();
  }


  @PostMapping("/draw/{lobbyId}")
  public ResponseEntity<Void> drawCard(@PathVariable String lobbyId, @RequestParam String playerId) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();
    if (game.getPlayers().size() <= 1) return ResponseEntity.status(409).build(); // Game over
    boolean endTurnEarly = false;

    int currentIndex = game.getCurrentPlayerIndex();
    if (currentIndex >= game.getPlayers().size()) return ResponseEntity.status(409).build();
    PlayerState currentPlayer = game.getPlayers().get(currentIndex);

    if(!currentPlayer.getPlayerId().equals(playerId)) {
      return ResponseEntity.status(403).build();
    }

    if (!game.getDeck().isEmpty()) {
      CardType drawnCard = game.getDeck().remove(0);
      endTurnEarly = gameService.handleDrawnCard(drawnCard, currentPlayer, game);

      gameActionService.recordActionWithPlayerIds(game.getGameId(), null, playerId, null, "DRAW_CARD", "{\"cardType\":\"" + drawnCard.name() + "\"}");

      // Send action notification for drawing a card
      Map<String, Object> actionData = new HashMap<>();
      actionData.put("message", currentPlayer.getPlayerName() + " drew a card from the deck");
      actionData.put("type", "info");
      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", actionData);
      
      // If the drawn card was an Exploding Kitten, send notification about getting exploding kitten
      if (drawnCard == CardType.EXPLODING_KITTEN) {
        Map<String, Object> explodingActionData = new HashMap<>();
        explodingActionData.put("message", currentPlayer.getPlayerName() + " got exploding kitten");
        explodingActionData.put("type", "error");
        messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", explodingActionData);
        
        // If player used Defuse, send additional notification
        if (endTurnEarly) {
          Map<String, Object> defuseActionData = new HashMap<>();
          defuseActionData.put("message", currentPlayer.getPlayerName() + " used DEFUSE");
          defuseActionData.put("type", "success");
          messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/action", defuseActionData);
        }
      }
      // currentPlayer.getHand().add(game.getDeck().remove(0));
      // game.setCardsToDraw(game.getCardsToDraw() - 1);
    }

    if (endTurnEarly || game.getCardsToDraw() <= 0) {
      // Game may have just ended (one player left)
      if (game.getPlayers().size() <= 1) {
        game.setCardsToDraw(0);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/state", "");
        return ResponseEntity.ok().build();
      }
      // If endTurnEarly is true, the GameService has already set the correct next player
      if (!endTurnEarly) {
        int next = findNextValidPlayerIndex(game, currentIndex);
        if (next == -1) return ResponseEntity.badRequest().build();
        game.setCurrentPlayerIndex(next);
        game.setCardsToDraw(1);
        messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", game.getPlayers().get(next).getPlayerId());
      } else {
        game.setCardsToDraw(1);
        int idx = game.getCurrentPlayerIndex();
        if (idx >= 0 && idx < game.getPlayers().size()) {
          messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", game.getPlayers().get(idx).getPlayerId());
        }
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



}
