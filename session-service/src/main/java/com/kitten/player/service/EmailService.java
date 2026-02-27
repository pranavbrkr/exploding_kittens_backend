package com.kitten.player.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender mailSender;

  public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  /**
   * Sends verification code to the user. If SMTP is not configured, logs the code to console (dev).
   */
  public void sendVerificationCode(String toEmail, String code) {
    if (mailSender != null) {
      try {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Exploding Kittens – Your verification code");
        msg.setText("Your verification code is: " + code + "\n\nIt expires in 15 minutes.");
        msg.setFrom("noreply@explodingkittens.local");
        mailSender.send(msg);
      } catch (Exception e) {
        log.warn("Failed to send verification email to {}, logging code instead: {}", toEmail, e.getMessage());
        log.info("Verification code for {}: {}", toEmail, code);
      }
    } else {
      log.info("SMTP not configured. Verification code for {}: {}", toEmail, code);
    }
  }
}
