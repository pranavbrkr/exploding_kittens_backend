package com.kitten.player.util;

import java.util.regex.Pattern;

/**
 * Validates email format (RFC 5322–style).
 * Rejects empty, too long, missing @, invalid local/domain, and no TLD.
 */
public final class EmailValidator {

  // Practical pattern: local part (allowed chars) @ domain (labels with dots), TLD at least 2 chars
  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$"
  );
  private static final int MAX_LENGTH = 254;

  private EmailValidator() {}

  public static boolean isValid(String email) {
    if (email == null || email.isBlank()) return false;
    String trimmed = email.trim();
    if (trimmed.length() > MAX_LENGTH) return false;
    if (trimmed.startsWith(".") || trimmed.contains("..") || trimmed.endsWith(".")) return false;
    return EMAIL_PATTERN.matcher(trimmed).matches();
  }
}
