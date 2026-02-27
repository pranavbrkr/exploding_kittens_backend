package com.kitten.player.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kitten.player.dto.PlayerResponse;
import com.kitten.player.service.PlayerService;

@RestController
@RequestMapping("/api/player")
@CrossOrigin
public class PlayerController {

  private final PlayerService playerService;

  public PlayerController(PlayerService playerService) {
    this.playerService = playerService;
  }

  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("ok");
  }

  @GetMapping("/{id}")
  public ResponseEntity<PlayerResponse> getPlayer(@PathVariable("id") String id) {
    PlayerResponse player = playerService.getPlayerById(id);
    if (player == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(player);
  }
}
