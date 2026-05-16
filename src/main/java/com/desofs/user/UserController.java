package com.desofs.user;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desofs.user.dto.RoleUpdateRequest;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody RoleUpdateRequest body) {
        try {
            User updated = userService.updateRole(id, body.getRole());
            return ResponseEntity.ok(Map.of(
                    "id", updated.getId(),
                    "email", updated.getEmail(),
                    "role", updated.getRole()
            ));
        } catch (RuntimeException ex) {
            if ("User not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
