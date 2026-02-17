package com.kitten.player.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kitten.player.dto.AuthRequest;
import com.kitten.player.dto.AuthResponse;
import com.kitten.player.model.User;
import com.kitten.player.repository.UserRepository;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public AuthResponse register(AuthRequest request) {
    if (request.getUsername() == null || request.getUsername().isBlank()) {
      throw new IllegalArgumentException("Username is required");
    }
    if (request.getPassword() == null || request.getPassword().length() < 4) {
      throw new IllegalArgumentException("Password must be at least 4 characters");
    }
    String username = request.getUsername().trim().toLowerCase();
    if (userRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("Username already taken");
    }
    String displayName = request.getDisplayName() != null && !request.getDisplayName().isBlank()
        ? request.getDisplayName().trim()
        : username;
    String hash = passwordEncoder.encode(request.getPassword());
    User user = new User(username, displayName, hash);
    user = userRepository.save(user);
    String token = jwtService.createToken(user.getId(), user.getUsername());
    return new AuthResponse(token, user.getId().toString(), user.getDisplayName());
  }

  public AuthResponse login(AuthRequest request) {
    if (request.getUsername() == null || request.getPassword() == null) {
      throw new IllegalArgumentException("Username and password are required");
    }
    User user = userRepository.findByUsername(request.getUsername().trim().toLowerCase())
        .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid username or password");
    }
    String token = jwtService.createToken(user.getId(), user.getUsername());
    return new AuthResponse(token, user.getId().toString(), user.getDisplayName());
  }
}
