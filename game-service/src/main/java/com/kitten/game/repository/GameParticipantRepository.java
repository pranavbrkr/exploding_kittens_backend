package com.kitten.game.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kitten.game.entity.GameParticipant;
import com.kitten.game.entity.GameParticipantId;

public interface GameParticipantRepository extends JpaRepository<GameParticipant, GameParticipantId> {

  List<GameParticipant> findByGameIdOrderBySeatIndex(UUID gameId);
}
