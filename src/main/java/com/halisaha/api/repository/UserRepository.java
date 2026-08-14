package com.halisaha.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halisaha.api.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
Optional<User> findByEmail(String email);
boolean existsByEmail(String email);
}
