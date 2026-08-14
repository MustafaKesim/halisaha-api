package com.halisaha.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.halisaha.api.dto.LoginRequest;
import com.halisaha.api.dto.LoginResponse;
import com.halisaha.api.dto.RegisterRequest;
import com.halisaha.api.dto.UserResponse;
import com.halisaha.api.entity.User;
import com.halisaha.api.security.JwtService;
import com.halisaha.api.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Kullanıcı İşlemleri", description = "Kayıt, giriş ve profil işlemleri")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @Operation(summary = "Yeni kullanıcı kaydı oluşturur")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Kayıt başarılı"),
            @ApiResponse(responseCode = "400", description = "Geçersiz veri (email formatı, eksik alan vb.)"),
            @ApiResponse(responseCode = "409", description = "Bu email zaten kayıtlı")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User savedUser = userService.register(request.email(), request.password(), request.fullName());

        UserResponse response = new UserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Email ve şifre ile giriş yapar, JWT token döner")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Giriş başarılı, token döndü"),
            @ApiResponse(responseCode = "400", description = "Geçersiz veri"),
            @ApiResponse(responseCode = "401", description = "Email veya şifre hatalı")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.email(), request.password());
        String token = jwtService.generateToken(user);

        LoginResponse response = new LoginResponse(token, user.getId(), user.getEmail(), user.getFullName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Giriş yapmış kullanıcının kendi bilgilerini döner")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kullanıcı bilgisi döndü"),
            @ApiResponse(responseCode = "403", description = "Geçerli bir token yok")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal User user) {
        UserResponse response = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCreatedAt()
        );

        return ResponseEntity.ok(response);
    }
}
