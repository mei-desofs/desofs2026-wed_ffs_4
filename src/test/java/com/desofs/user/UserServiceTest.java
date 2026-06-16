package com.desofs.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.desofs.user.model.User;
import com.desofs.user.repository.UserRepository;
import com.desofs.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void updateRoleShouldUpdateUserRole() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setRole("USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateRole(1L, "manager");

        assertEquals("MANAGER", result.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void updateRoleShouldRejectInvalidRole() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.updateRole(1L, "INVALID"));

        assertEquals("Invalid role", ex.getMessage());
    }

    @Test
    void updateRoleShouldThrowWhenUserNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.updateRole(2L, "ADMIN"));

        assertEquals("User not found", ex.getMessage());
    }
}
