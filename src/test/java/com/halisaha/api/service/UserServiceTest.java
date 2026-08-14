package com.halisaha.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.halisaha.api.entity.User;
import com.halisaha.api.exception.ConflictException;
import com.halisaha.api.exception.UnauthorizedException;
import com.halisaha.api.repository.UserRepository;
import com.halisaha.api.security.LoginRateLimiter;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @InjectMocks
    private UserService userService;

    @Test
    void register_savesUserWithHashedPassword_whenEmailNotTaken() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register("test@example.com", "plainPassword", "Test User");

        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getPassword()).isEqualTo("hashedPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsConflictException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("taken@example.com", "pw", "Name"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Bu email adresi zaten kayıtlı");
    }

    @Test
    void login_returnsUser_whenCredentialsCorrect() {
        User user = User.builder().email("test@example.com").password("hashedPassword").fullName("Test").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPassword", "hashedPassword")).thenReturn(true);

        User result = userService.login("test@example.com", "plainPassword");

        assertThat(result).isEqualTo(user);
        verify(loginRateLimiter).checkAllowed("test@example.com");
        verify(loginRateLimiter).recordSuccess("test@example.com");
    }

    @Test
    void login_throwsUnauthorized_whenPasswordWrong() {
        User user = User.builder().email("test@example.com").password("hashedPassword").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.login("test@example.com", "wrongPassword"))
                .isInstanceOf(UnauthorizedException.class);
        verify(loginRateLimiter).recordFailure("test@example.com");
    }

    @Test
    void login_throwsUnauthorized_whenEmailNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login("missing@example.com", "any"))
                .isInstanceOf(UnauthorizedException.class);
        verify(loginRateLimiter).recordFailure("missing@example.com");
    }

    @Test
    void login_throwsTooManyRequests_whenRateLimited() {
        org.mockito.Mockito.doThrow(new com.halisaha.api.exception.TooManyRequestsException("Çok fazla deneme"))
                .when(loginRateLimiter).checkAllowed("blocked@example.com");

        assertThatThrownBy(() -> userService.login("blocked@example.com", "any"))
                .isInstanceOf(com.halisaha.api.exception.TooManyRequestsException.class);

        org.mockito.Mockito.verifyNoInteractions(userRepository);
    }
}
