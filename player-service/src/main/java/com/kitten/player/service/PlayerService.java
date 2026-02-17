package com.kitten.player.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.kitten.player.dto.PlayerResponse;
import com.kitten.player.model.User;
import com.kitten.player.repository.UserRepository;

@Service
public class PlayerService {

  private final UserRepository userRepository;

  public PlayerService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public PlayerResponse getPlayerById(String id) {
    try {
      UUID uuid = UUID.fromString(id);
      return userRepository.findById(uuid)
          .map(user -> new PlayerResponse(user.getId().toString(), user.getDisplayName()))
          .orElse(null);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
