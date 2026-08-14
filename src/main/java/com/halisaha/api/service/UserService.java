package com.halisaha.api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.halisaha.api.entity.User;
import com.halisaha.api.exception.ConflictException;
import com.halisaha.api.exception.UnauthorizedException;
import com.halisaha.api.repository.UserRepository;
import com.halisaha.api.security.LoginRateLimiter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;

    public User register(String email, String rawPassword, String fullName) {

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Bu email adresi zaten kayıtlı");
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = User.builder()
                .email(email)
                .password(hashedPassword)
                .fullName(fullName)
                .build();
        return userRepository.save(user);
    }

    public User login(String email, String rawPassword) {
        loginRateLimiter.checkAllowed(email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            loginRateLimiter.recordFailure(email);
            throw new UnauthorizedException("Email veya şifre hatalı");
        }

        loginRateLimiter.recordSuccess(email);
        return user;
    }
}
