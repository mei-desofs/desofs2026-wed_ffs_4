package com.desofs.auth;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desofs.security.JwtUtil;
import com.desofs.security.TokenBlacklistService;
import com.desofs.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String password = body.get("password");
            User user = authService.register(email, password);
            return ResponseEntity.status(201).body(Map.of("id", user.getId(), "email", user.getEmail()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String password = body.get("password");
            Map<String, String> tokens = authService.login(email, password);
            return ResponseEntity.ok(tokens);
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            Map<String, String> tokens = authService.refresh(refreshToken);
            return ResponseEntity.ok(tokens);
        } catch (Exception ex) {
            return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing token"));
        }

        String token = authHeader.substring(7);
        try {
            Jws<Claims> claims = jwtUtil.validate(token);
            tokenBlacklistService.blacklist(token, claims.getBody().getExpiration().toInstant());
            return ResponseEntity.ok(Map.of("message", "Logged out"));
        } catch (JwtException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }
}
