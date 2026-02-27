package com.kitten.player.util;

import java.util.regex.Pattern;

/**
 * Validates password strength: at least 8 characters, with at least one letter,
 * one digit, and one special character.
 */
public final class PasswordValidator {

  private static final int MIN_LENGTH = 8;
  private static final Pattern HAS_LETTER = Pattern.compile("[a-zA-Z]");
  private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
  private static final Pattern HAS_SPECIAL = Pattern.compile("[^a-zA-Z0-9]");

  private PasswordValidator() {}

  public static boolean isValid(String password) {
    if (password == null || password.length() < MIN_LENGTH) return false;
    return HAS_LETTER.matcher(password).find()
        && HAS_DIGIT.matcher(password).find()
        && HAS_SPECIAL.matcher(password).find();
  }

  public static String getRequirementMessage() {
    return "Password must be at least 8 characters and include a letter, a number, and a special character";
  }
}
