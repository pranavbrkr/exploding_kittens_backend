package com.kitten.player.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kitten.player.dto.AuthRequest;
import com.kitten.player.dto.AuthResponse;
import com.kitten.player.dto.PlayerResponse;
import com.kitten.player.model.User;
import com.kitten.player.repository.UserRepository;
import com.kitten.player.service.AuthService;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

  private final AuthService authService;
  private final UserRepository userRepository;

  public AuthController(AuthService authService, UserRepository userRepository) {
    this.authService = authService;
    this.userRepository = userRepository;
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody AuthRequest request) {
    try {
      AuthResponse response = authService.register(request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
    }
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody AuthRequest request) {
    try {
      AuthResponse response = authService.login(request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(401).body(java.util.Map.of("message", e.getMessage()));
    }
  }

  @GetMapping("/me")
  public ResponseEntity<PlayerResponse> me(@AuthenticationPrincipal UUID userId) {
    if (userId == null) {
      return ResponseEntity.status(401).build();
    }
    return userRepository.findById(userId)
        .map(user -> ResponseEntity.ok(new PlayerResponse(user.getId().toString(), user.getDisplayName())))
        .orElse(ResponseEntity.status(401).build());
  }
}
