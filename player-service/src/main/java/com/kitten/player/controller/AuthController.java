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
import com.kitten.player.dto.CompleteRegistrationRequest;
import com.kitten.player.dto.PlayerResponse;
import com.kitten.player.dto.RegisterRequest;
import com.kitten.player.dto.RegisterResponse;
import com.kitten.player.dto.VerifyEmailRequest;
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
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    try {
      RegisterResponse response = authService.register(request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
    }
  }

  @PostMapping("/verify-email")
  public ResponseEntity<?> verifyEmail(@RequestBody VerifyEmailRequest request) {
    try {
      AuthResponse response = authService.verifyEmail(request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
    }
  }

  @PostMapping("/complete-registration")
  public ResponseEntity<?> completeRegistration(
      @AuthenticationPrincipal Object principal,
      @RequestBody CompleteRegistrationRequest request) {
    if (principal == null) {
      return ResponseEntity.status(401).body(java.util.Map.of("message", "Not authenticated"));
    }
    String subject = principal.toString();
    if (subject.startsWith("guest_")) {
      return ResponseEntity.badRequest().body(java.util.Map.of("message", "Guests cannot complete registration"));
    }
    try {
      UUID userId = UUID.fromString(subject);
      AuthResponse response = authService.completeRegistration(userId, request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
    }
  }

  @PostMapping("/guest")
  public ResponseEntity<?> guest() {
    try {
      AuthResponse response = authService.createGuestSession();
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      return ResponseEntity.status(500).body(java.util.Map.of("message", "Failed to create guest session"));
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
  public ResponseEntity<PlayerResponse> me(@AuthenticationPrincipal Object principal) {
    if (principal == null) {
      return ResponseEntity.status(401).build();
    }
    String subject = principal.toString();
    if (subject.startsWith("guest_")) {
      String name = authService.getGuestName(subject);
      return ResponseEntity.ok(new PlayerResponse(subject, name != null ? name : "Guest"));
    }
    try {
      UUID userId = UUID.fromString(subject);
      return userRepository.findById(userId)
          .map(user -> ResponseEntity.ok(new PlayerResponse(user.getId().toString(), user.getEffectiveDisplayName())))
          .orElse(ResponseEntity.status(401).build());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(401).build();
    }
  }
}
