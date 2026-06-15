package com.desofs.user.controller;

import java.util.Map;

import com.desofs.user.model.User;
import com.desofs.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desofs.user.dto.UsernameUpdateRequest;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {
    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        try {
            User user = userService.getById(id);
            return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getEmail(), "role", user.getRole()));
        } catch (RuntimeException ex) {
            if ("User not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UsernameUpdateRequest body) {
        try {
            User updated = userService.updateUsername(id, body.getUsername(), currentUserEmail());
            return ResponseEntity.ok(Map.of("id", updated.getId(), "username", updated.getEmail(), "role", updated.getRole()));
        } catch (RuntimeException ex) {
            if ("User not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
            }
            if ("Forbidden".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
            }
            if ("Email already in use".equals(ex.getMessage())) {
                return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        throw new RuntimeException("Unauthorized");
    }
}