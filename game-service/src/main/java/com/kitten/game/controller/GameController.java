package com.kitten.game.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kitten.game.model.GameState;
import com.kitten.game.service.GameService;

@RestController
@RequestMapping("/game")
public class GameController {

  @Autowired
  private GameService gameService;

  @PostMapping("/start")
  public GameState startGame(@RequestParam("lobbyId") String lobbyId, @RequestBody List<String> playerIds) {
    return gameService.startGame(lobbyId, playerIds);
  }

  @GetMapping("/{lobbyId}")
  public ResponseEntity<GameState> getGameState(@PathVariable("lobbyId") String lobbyId) {
    GameState game = gameService.getGame(lobbyId);
    if (game == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(game);
  }
    
}
