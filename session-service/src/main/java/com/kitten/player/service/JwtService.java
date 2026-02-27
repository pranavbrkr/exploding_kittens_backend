package com.kitten.player.service;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.kitten.player.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

  private final JwtProperties properties;
  private final SecretKey key;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
    this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  public String createToken(UUID userId, String username) {
    return createToken(userId.toString(), username);
  }

  /** Create token with string subject (for guests: guest_&lt;uuid&gt;). */
  public String createToken(String subject, String name) {
    return Jwts.builder()
        .subject(subject)
        .claim("username", name)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + properties.getExpirationMs()))
        .signWith(key)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public UUID getUserIdFromToken(String token) {
    String sub = parseToken(token).getSubject();
    if (sub.startsWith("guest_")) {
      throw new IllegalArgumentException("Guest token has no UUID subject");
    }
    return UUID.fromString(sub);
  }

  /** Subject from token (UUID string for users, guest_&lt;uuid&gt; for guests). */
  public String getSubject(String token) {
    return parseToken(token).getSubject();
  }

  public boolean isValidToken(String token) {
    try {
      parseToken(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
