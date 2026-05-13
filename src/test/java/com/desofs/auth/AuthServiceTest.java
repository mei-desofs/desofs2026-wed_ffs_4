package com.desofs.auth;

import com.desofs.security.JwtUtil;
import com.desofs.user.User;
import com.desofs.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldCreateUserWithEncodedPasswordAndUserRole() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register("user@example.com", "password123");

        assertNotNull(result);
        assertEquals("user@example.com", result.getEmail());
        assertEquals("encoded-password", result.getPassword());
        assertEquals("USER", result.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("user@example.com", "password123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginShouldReturnJwtTokenWhenCredentialsAreValid() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("encoded-password");
        user.setRole("USER");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("user@example.com", "USER")).thenReturn("jwt-token");

        String token = authService.login("user@example.com", "password123");

        assertEquals("jwt-token", token);
        verify(jwtUtil).generateToken("user@example.com", "USER");
    }

    @Test
    void loginShouldRejectInvalidCredentials() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("encoded-password");
        user.setRole("USER");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.login("user@example.com", "wrong-password"));

        verify(jwtUtil, never()).generateToken(any(), any());
    }
}
