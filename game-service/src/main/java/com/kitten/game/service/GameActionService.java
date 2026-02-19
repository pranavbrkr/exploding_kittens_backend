package com.kitten.game.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kitten.game.entity.GameAction;
import com.kitten.game.repository.GameActionRepository;

@Service
public class GameActionService {

  private static final Logger log = LoggerFactory.getLogger(GameActionService.class);
  private final GameActionRepository gameActionRepository;

  public GameActionService(GameActionRepository gameActionRepository) {
    this.gameActionRepository = gameActionRepository;
  }

  /**
   * Appends an action to the game's event log. No-op if gameIdStr is null or invalid.
   *
   * @param gameIdStr    persisted game UUID (from GameState.gameId), or null to skip
   * @param turnNumber   optional turn number (can be null)
   * @param actorUserId  user who performed the action (null for system)
   * @param receiverUserId  target user when applicable (favor, attack, targeted attack, cat steal)
   * @param actionType   e.g. DRAW_CARD, PLAY_SKIP, PLAY_ATTACK, PLAY_FAVOR, TARGETED_ATTACK_CONFIRM, FAVOR_RESPONSE, CAT_STEAL, ELIMINATED, GAME_START
   * @param payload      optional JSON string for extra data (card types, etc.)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordAction(String gameIdStr, Integer turnNumber, UUID actorUserId, UUID receiverUserId, String actionType, String payload) {
    if (gameIdStr == null || gameIdStr.isBlank()) {
      log.debug("Game action skipped: gameId is null or blank (actionType={})", actionType);
      return;
    }
    try {
      UUID gameId = UUID.fromString(gameIdStr.trim());
      int nextSeq = gameActionRepository.findMaxSeqByGameId(gameId) + 1;
      GameAction action = new GameAction(UUID.randomUUID(), gameId, nextSeq, actionType);
      action.setTurnNumber(turnNumber);
      action.setActorUserId(actorUserId);
      action.setReceiverUserId(receiverUserId);
      action.setPayload(payload);
      gameActionRepository.save(action);
    } catch (Exception e) {
      log.warn("Failed to persist game action: gameId={}, actionType={}, error={}", gameIdStr, actionType, e.getMessage(), e);
    }
  }

  /** Convenience: record with actor/receiver as UUID strings (playerIds). */
  public void recordActionWithPlayerIds(String gameIdStr, Integer turnNumber, String actorUserIdStr, String receiverUserIdStr, String actionType, String payload) {
    UUID actor = parseUuid(actorUserIdStr);
    UUID receiver = parseUuid(receiverUserIdStr);
    recordAction(gameIdStr, turnNumber, actor, receiver, actionType, payload);
  }

  private static UUID parseUuid(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return UUID.fromString(s.trim());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
