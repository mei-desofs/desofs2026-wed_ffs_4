package com.desofs.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenDurationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                              @Value("${jwt.refresh-expiration-ms:604800000}") long refreshTokenDurationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDurationMs = refreshTokenDurationMs;
    }

    public RefreshToken createRefreshToken(String userEmail) {
        String token = generateRandomToken();
        Instant expiry = Instant.now().plusMillis(refreshTokenDurationMs);
        refreshTokenRepository.deleteByUserEmail(userEmail);
        RefreshToken refreshToken = new RefreshToken(token, userEmail, expiry);
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public void deleteByUserEmail(String userEmail) {
        refreshTokenRepository.deleteByUserEmail(userEmail);
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
