package com.kitten.game.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kitten.game.entity.GameAction;

public interface GameActionRepository extends JpaRepository<GameAction, UUID> {

  List<GameAction> findByGameIdOrderBySeqAsc(UUID gameId);

  @Query("SELECT COALESCE(MAX(ga.seq), 0) FROM GameAction ga WHERE ga.gameId = :gameId")
  int findMaxSeqByGameId(@Param("gameId") UUID gameId);
}
