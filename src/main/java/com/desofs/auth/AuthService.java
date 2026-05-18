package com.desofs.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.desofs.audit.AuditAction;
import com.desofs.audit.AuditService;
import com.desofs.security.JwtUtil;
import com.desofs.user.User;
import com.desofs.user.UserRepository;

@Service
public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_LOGIN_FAILURES = 5;
    private static final java.time.Duration LOCKOUT_DURATION = java.time.Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final ConcurrentMap<String, LoginState> loginStates = new ConcurrentHashMap<>();

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, RefreshTokenService refreshTokenService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
    }

    public User register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            recordAudit(email, AuditAction.REGISTER, "user", email, false, "Email already in use");
            throw new IllegalArgumentException("Email already in use");
        }
        validatePassword(password);
        User u = new User();
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRole("USER");
        User saved = userRepository.save(u);
        recordAudit(email, AuditAction.REGISTER, "user", String.valueOf(saved.getId()), true, "Registered successfully");
        return saved;
    }

    public Map<String, String> login(String email, String password) {
        LoginState state = loginStates.computeIfAbsent(email, key -> new LoginState());
        if (state.isLocked()) {
            recordAudit(email, AuditAction.LOCKOUT, "auth", email, false, "Account temporarily locked");
            throw new AccountLockedException("Too many failed login attempts. Try again later");
        }

        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            state.registerFailure();
            recordAudit(email, AuditAction.LOGIN_FAILURE, "auth", email, false, "Invalid password");
            if (state.isLocked()) {
                recordAudit(email, AuditAction.LOCKOUT, "auth", email, false, "Account temporarily locked");
                throw new AccountLockedException("Too many failed login attempts. Try again later");
            }
            throw new IllegalArgumentException("Invalid credentials");
        }
        state.reset();
        String jwt = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail()).getToken();
        recordAudit(email, AuditAction.LOGIN_SUCCESS, "auth", email, true, "Successful login");
        return Map.of("token", jwt, "refreshToken", refreshToken);
    }

    public Map<String, String> refresh(String refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .filter(rt -> rt.getExpiryDate().isAfter(java.time.Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));
        User user = userRepository.findByEmail(token.getUserEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String jwt = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String newRefreshToken = refreshTokenService.createRefreshToken(user.getEmail()).getToken();
        recordAudit(user.getEmail(), AuditAction.REFRESH, "auth", user.getEmail(), true, "Refresh token rotated");
        return Map.of("token", jwt, "refreshToken", newRefreshToken);
    }

    private void recordAudit(String actor, AuditAction action, String resourceType, String resourceId, boolean success, String details) {
        if (auditService != null) {
            auditService.record(actor, action, resourceType, resourceId, success, details);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!containsLetter(password) || !containsDigit(password)) {
            throw new IllegalArgumentException("Password must contain at least one letter and one digit");
        }
    }

    private boolean containsLetter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDigit(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static final class LoginState {
        private int failedAttempts;
        private java.time.Instant lockedUntil;

        boolean isLocked() {
            return lockedUntil != null && lockedUntil.isAfter(java.time.Instant.now());
        }

        void registerFailure() {
            failedAttempts++;
            if (failedAttempts >= MAX_LOGIN_FAILURES) {
                lockedUntil = java.time.Instant.now().plus(LOCKOUT_DURATION);
            }
        }

        void reset() {
            failedAttempts = 0;
            lockedUntil = null;
        }
    }
}
