package com.kitten.game.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kitten.game.entity.Game;

public interface GameRepository extends JpaRepository<Game, UUID> {

  Optional<Game> findByLobbyId(String lobbyId);
}
