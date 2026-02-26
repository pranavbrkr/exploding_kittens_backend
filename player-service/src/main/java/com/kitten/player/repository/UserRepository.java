package com.kitten.player.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kitten.player.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<User> findByEmailAndVerificationCode(String email, String code);
}
