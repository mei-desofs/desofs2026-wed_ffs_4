package com.desofs.auth;

import com.desofs.user.User;
import com.desofs.user.UserRepository;
import com.desofs.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RefreshTokenService refreshTokenService;
    @InjectMocks
    private AuthService authService;

    @Test
    void loginShouldReturnJwtAndRefreshToken() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("encoded-password");
        user.setRole("USER");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("user@example.com", "USER")).thenReturn("jwt-token");
        RefreshToken refreshToken = new RefreshToken("refresh-token", "user@example.com", Instant.now().plusSeconds(1000));
        when(refreshTokenService.createRefreshToken("user@example.com")).thenReturn(refreshToken);
        Map<String, String> tokens = authService.login("user@example.com", "password123");
        assertEquals("jwt-token", tokens.get("token"));
        assertEquals("refresh-token", tokens.get("refreshToken"));
    }

    @Test
    void refreshShouldReturnNewTokens() {
        RefreshToken oldToken = new RefreshToken("refresh-token", "user@example.com", Instant.now().plusSeconds(1000));
        when(refreshTokenService.findByToken("refresh-token")).thenReturn(Optional.of(oldToken));
        User user = new User();
        user.setEmail("user@example.com");
        user.setRole("USER");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("user@example.com", "USER")).thenReturn("jwt-token");
        RefreshToken newToken = new RefreshToken("new-refresh", "user@example.com", Instant.now().plusSeconds(1000));
        when(refreshTokenService.createRefreshToken("user@example.com")).thenReturn(newToken);
        Map<String, String> tokens = authService.refresh("refresh-token");
        assertEquals("jwt-token", tokens.get("token"));
        assertEquals("new-refresh", tokens.get("refreshToken"));
    }

    @Test
    void refreshShouldThrowOnInvalidToken() {
        when(refreshTokenService.findByToken("bad-token")).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.refresh("bad-token"));
        assertEquals("Invalid or expired refresh token", ex.getMessage());
    }
}
