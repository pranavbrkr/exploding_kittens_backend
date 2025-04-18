package com.kitten.player.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kitten.player.dto.PlayerRequest;
import com.kitten.player.dto.PlayerResponse;
import com.kitten.player.model.Player;
import com.kitten.player.service.PlayerService;

@RestController
@RequestMapping("/player")
@CrossOrigin
public class PlayerController {
  
  @Autowired
  private PlayerService playerService;

  @PostMapping("/register")
  public PlayerResponse register(@RequestBody PlayerRequest request) {
    Player player = playerService.createPlayer(request.getName());
    return new PlayerResponse(player.getId(), player.getName());
  }
}
