package com.kitten.player.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kitten.player.dto.AuthRequest;
import com.kitten.player.dto.AuthResponse;
import com.kitten.player.dto.CompleteRegistrationRequest;
import com.kitten.player.dto.RegisterRequest;
import com.kitten.player.dto.RegisterResponse;
import com.kitten.player.dto.VerifyEmailRequest;
import com.kitten.player.model.User;
import com.kitten.player.repository.UserRepository;
import com.kitten.player.util.EmailValidator;
import com.kitten.player.util.PasswordValidator;

@Service
public class AuthService {

  private static final int CODE_EXPIRY_MINUTES = 15;
  private static final int CODE_LENGTH = 6;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final EmailService emailService;
  private final GuestStore guestStore;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                     JwtService jwtService, EmailService emailService, GuestStore guestStore) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.emailService = emailService;
    this.guestStore = guestStore;
  }

  /** Create a guest session: no DB user, JWT with guest id and generated name. */
  public AuthResponse createGuestSession() {
    String guestId = "guest_" + java.util.UUID.randomUUID();
    String name = "guest_" + (int) (Math.random() * 900_000 + 100_000); // 6-digit
    guestStore.put(guestId, name);
    String token = jwtService.createToken(guestId, name);
    return new AuthResponse(token, guestId, name);
  }

  public String getGuestName(String guestId) {
    return guestStore.get(guestId);
  }

  /** Step 1: Register with email + password. Sends verification code; no JWT yet. */
  public RegisterResponse register(RegisterRequest request) {
    if (request.getEmail() == null || request.getEmail().isBlank()) {
      throw new IllegalArgumentException("Email is required");
    }
    if (request.getPassword() == null || request.getPassword().isEmpty()) {
      throw new IllegalArgumentException("Password is required");
    }
    if (!PasswordValidator.isValid(request.getPassword())) {
      throw new IllegalArgumentException(PasswordValidator.getRequirementMessage());
    }
    String email = request.getEmail().trim().toLowerCase();
    if (!EmailValidator.isValid(email)) {
      throw new IllegalArgumentException("Invalid email address");
    }
    if (userRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("An account with this email already exists");
    }
    String hash = passwordEncoder.encode(request.getPassword());
    User user = new User(email, hash);
    String code = generateNumericCode(CODE_LENGTH);
    user.setVerificationCode(code);
    user.setVerificationCodeExpiresAt(Instant.now().plusSeconds(CODE_EXPIRY_MINUTES * 60L));
    userRepository.save(user);
    emailService.sendVerificationCode(email, code);
    return new RegisterResponse("Verification code sent to your email. Check your inbox (and spam).");
  }

  /** Step 2: Verify email with code. Returns JWT; may need game name (needsGameName=true). */
  public AuthResponse verifyEmail(VerifyEmailRequest request) {
    if (request.getEmail() == null || request.getEmail().isBlank()) {
      throw new IllegalArgumentException("Email is required");
    }
    if (request.getCode() == null || request.getCode().isBlank()) {
      throw new IllegalArgumentException("Verification code is required");
    }
    String email = request.getEmail().trim().toLowerCase();
    String code = request.getCode().trim();
    User user = userRepository.findByEmailAndVerificationCode(email, code)
        .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code"));
    if (user.getVerificationCodeExpiresAt() != null && user.getVerificationCodeExpiresAt().isBefore(Instant.now())) {
      throw new IllegalArgumentException("Verification code has expired");
    }
    user.setEmailVerified(true);
    user.setVerificationCode(null);
    user.setVerificationCodeExpiresAt(null);
    userRepository.save(user);
    String token = jwtService.createToken(user.getId(), user.getEmail());
    boolean needsGameName = user.getUsername() == null || user.getUsername().isBlank();
    String name = user.getEffectiveDisplayName();
    return new AuthResponse(token, user.getId().toString(), name, needsGameName);
  }

  /** Step 3: Set game name (unique). Requires JWT from verify-email. User must be verified and not yet have a game name. */
  public AuthResponse completeRegistration(UUID userId, CompleteRegistrationRequest request) {
    if (request.getGameName() == null || request.getGameName().isBlank()) {
      throw new IllegalArgumentException("Game name is required");
    }
    String gameName = request.getGameName().trim();
    if (gameName.length() < 2) {
      throw new IllegalArgumentException("Game name must be at least 2 characters");
    }
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (!user.isEmailVerified()) {
      throw new IllegalArgumentException("Email must be verified first");
    }
    if (user.getUsername() != null && !user.getUsername().isBlank()) {
      throw new IllegalArgumentException("Game name already set");
    }
    String normalized = gameName.toLowerCase();
    if (userRepository.existsByUsername(normalized)) {
      throw new IllegalArgumentException("This game name is already taken");
    }
    user.setUsername(normalized);
    user.setDisplayName(gameName);
    userRepository.save(user);
    String token = jwtService.createToken(user.getId(), user.getUsername());
    return new AuthResponse(token, user.getId().toString(), user.getDisplayName(), false);
  }

  /** Login with email + password. User must be fully registered (verified + game name set). */
  public AuthResponse login(AuthRequest request) {
    if (request.getEmail() == null || request.getEmail().isBlank()) {
      throw new IllegalArgumentException("Email is required");
    }
    if (request.getPassword() == null) {
      throw new IllegalArgumentException("Password is required");
    }
    String input = request.getEmail().trim().toLowerCase();
    // Support login by email or by username (game name) for backward compatibility
    User user = (input.contains("@")
        ? userRepository.findByEmail(input)
        : userRepository.findByUsername(input))
        .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
    boolean legacyUser = user.getEmail() == null || user.getEmail().isBlank();
    if (!legacyUser && !user.isEmailVerified()) {
      throw new IllegalArgumentException("Please verify your email first. Check your inbox for the verification code.");
    }
    if (!legacyUser && (user.getUsername() == null || user.getUsername().isBlank())) {
      throw new IllegalArgumentException("Please complete registration by setting your game name.");
    }
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid email or password");
    }
    String token = jwtService.createToken(user.getId(), user.getUsername());
    return new AuthResponse(token, user.getId().toString(), user.getEffectiveDisplayName());
  }

  private static String generateNumericCode(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append((int) (Math.random() * 10));
    }
    return sb.toString();
  }
}
