package com.desofs.user;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.desofs.audit.AuditAction;
import com.desofs.audit.AuditService;
import com.desofs.security.TokenBlacklistService;

@Service
public class UserService {
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "MANAGER", "USER");

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TokenBlacklistService tokenBlacklistService;

    public UserService(UserRepository userRepository, AuditService auditService, TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.tokenBlacklistService = tokenBlacklistService;
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
        User saved = userRepository.save(user);
        recordAudit(saved.getEmail(), AuditAction.ROLE_CHANGE, "user", String.valueOf(saved.getId()), true, "Role changed to " + normalizedRole);
        return saved;
    }

    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUsername(Long userId, String username, String currentUserEmail) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!target.getEmail().equals(currentUserEmail) && !"ADMIN".equals(findRoleByEmail(currentUserEmail))) {
            throw new RuntimeException("Forbidden");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (userRepository.findByEmail(username).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        target.setEmail(username.trim());
        return userRepository.save(target);
    }

    private String findRoleByEmail(String email) {
        return userRepository.findByEmail(email).map(User::getRole).orElse("USER");
    }

    private void recordAudit(String actor, AuditAction action, String resourceType, String resourceId, boolean success, String details) {
        if (auditService != null) {
            auditService.record(actor, action, resourceType, resourceId, success, details);
        }
    }
    public void terminateUserSessions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        tokenBlacklistService.blacklistAllForUser(user.getEmail());
        recordAudit(user.getEmail(), AuditAction.SESSION_TERMINATE, "auth",
                String.valueOf(userId), true, "All sessions terminated by admin");
    }

    public void terminateAllSessions() {
        userRepository.findAll().forEach(user ->
                tokenBlacklistService.blacklistAllForUser(user.getEmail()));
        recordAudit("admin", AuditAction.SESSION_TERMINATE, "auth",
                "all", true, "All user sessions terminated by admin");
    }
}
