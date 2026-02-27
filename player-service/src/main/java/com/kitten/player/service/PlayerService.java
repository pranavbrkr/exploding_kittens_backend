package com.kitten.player.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.kitten.player.dto.PlayerResponse;
import com.kitten.player.model.User;
import com.kitten.player.repository.UserRepository;

@Service
public class PlayerService {

  private final UserRepository userRepository;
  private final GuestStore guestStore;

  public PlayerService(UserRepository userRepository, GuestStore guestStore) {
    this.userRepository = userRepository;
    this.guestStore = guestStore;
  }

  public PlayerResponse getPlayerById(String id) {
    if (id != null && id.startsWith("guest_")) {
      String name = guestStore.getOrDefault(id, "Guest");
      return new PlayerResponse(id, name);
    }
    try {
      UUID uuid = UUID.fromString(id);
      return userRepository.findById(uuid)
          .map(user -> new PlayerResponse(user.getId().toString(), user.getEffectiveDisplayName()))
          .orElse(null);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
