package com.kitten.game.controller;

import java.util.Collections;
import java.util.List;

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

  @PostMapping("/skip/{lobbyId}")
  public ResponseEntity<Void> skipTurn(@PathVariable String lobbyId) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) return ResponseEntity.notFound().build();

    int next = (game.getCurrentPlayerIndex() + 1) % game.getPlayers().size();
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
      int next = (currentIndex + 1) % game.getPlayers().size();
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
      int next = (currentIndex + 1) % game.getPlayers().size();
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
      int next = (currentIndex + 1) % game.getPlayers().size();
      game.setCurrentPlayerIndex(next);
      game.setCardsToDraw(1);

      messagingTemplate.convertAndSend("/topic/game/" + lobbyId + "/turn", game.getPlayers().get(next).getPlayerId());
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
