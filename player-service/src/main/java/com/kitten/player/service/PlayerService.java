package com.kitten.player.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kitten.player.model.Player;

@Service
public class PlayerService {
  private final Map<String, Player> playerStore = new HashMap<>();

  public Player createPlayer(String name) {
    Player player = new Player(name);
    playerStore.put(player.getId(), player);
    return player;
  }

  public Player getPlayerById(String id) {
    return playerStore.get(id);
  }
}
