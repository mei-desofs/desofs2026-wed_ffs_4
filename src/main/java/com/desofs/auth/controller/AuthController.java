package com.desofs.auth.controller;

import java.util.Map;

import com.desofs.auth.AccountLockedException;
import com.desofs.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desofs.audit.model.AuditAction;
import com.desofs.audit.service.AuditService;
import com.desofs.security.JwtUtil;
import com.desofs.security.TokenBlacklistService;
import com.desofs.user.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditService auditService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService, AuditService auditService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String email = resolveLoginIdentifier(body);
            String password = body.get("password");
            User user = authService.register(email, password);
            auditService.record(email, AuditAction.REGISTER, "user", String.valueOf(user.getId()), true, "User registered");
            return ResponseEntity.status(201).body(Map.of("id", user.getId(), "email", user.getEmail()));
        } catch (Exception ex) {
            auditService.record(resolveLoginIdentifier(body), AuditAction.REGISTER, "user", "-", false, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email = resolveLoginIdentifier(body);
            String password = body.get("password");
            Map<String, String> tokens = authService.login(email, password);
            return ResponseEntity.ok(tokens);
        } catch (Exception ex) {
            if (ex instanceof AccountLockedException) {
                auditService.record(resolveLoginIdentifier(body), AuditAction.LOCKOUT, "auth", "-", false, ex.getMessage());
                return ResponseEntity.status(429).body(Map.of("error", ex.getMessage()));
            }
            auditService.record(resolveLoginIdentifier(body), AuditAction.LOGIN_FAILURE, "auth", "-", false, ex.getMessage());
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
            String userEmail = claims.getBody().getSubject();
            if (userEmail == null || userEmail.isBlank()) {
                throw new JwtException("Missing token subject");
            }
            tokenBlacklistService.blacklist(token, claims.getBody().getExpiration().toInstant());
            authService.revokeRefreshTokensForUser(userEmail);
            auditService.record(userEmail, AuditAction.LOGOUT, "auth", userEmail, true, "Logged out");
            return ResponseEntity.ok(Map.of("message", "Logged out"));
        } catch (JwtException ex) {
            auditService.record("unknown", AuditAction.LOGOUT, "auth", "-", false, ex.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }
    }

    private String resolveLoginIdentifier(Map<String, String> body) {
        String username = body.get("username");
        if (username != null && !username.isBlank()) {
            return username;
        }
        return body.get("email");
    }
}
