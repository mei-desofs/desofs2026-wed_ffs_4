package com.desofs.auth;

import com.desofs.user.User;
import com.desofs.user.UserRepository;
import com.desofs.security.JwtUtil;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    public User register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        User u = new User();
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRole("USER");
        return userRepository.save(u);
    }

    public Map<String, String> login(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        String jwt = jwtUtil.generateToken(user.getEmail(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getEmail()).getToken();
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
        return Map.of("token", jwt, "refreshToken", newRefreshToken);
    }
}
