package com.desofs.auth;

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

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> authService.register("user@example.com", "password123"));
        assertEquals("Email already in use", ex.getMessage());

        verify(userRepository, never()).save(any());
    }

}
