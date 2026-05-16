package com.desofs.user;

import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "MANAGER", "USER");

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User updateRole(Long userId, String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }

        String normalizedRole = role.trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Invalid role");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(normalizedRole);
        return userRepository.save(user);
    }
}
