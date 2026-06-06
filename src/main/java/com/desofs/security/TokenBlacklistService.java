package com.desofs.security;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenBlacklistService {
    private final TokenBlacklistRepository repository;

    public TokenBlacklistService(TokenBlacklistRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void blacklist(String token, Instant expiresAt) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Token expiration is required");
        }
        if (!repository.existsByToken(token)) {
            repository.save(new TokenBlacklist(token, expiresAt));
        }
    }

    @Transactional
    public void blacklistAllForUser(String userEmail) {
        repository.deleteByUserEmail(userEmail);
        TokenBlacklist entry = new TokenBlacklist("revoke-all:" + userEmail, Instant.now().plus(java.time.Duration.ofDays(1)), userEmail);
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public boolean isUserRevoked(String userEmail) {
        return repository.existsByUserEmail(userEmail);
    }
    
    public boolean isBlacklisted(String token) {
    if (token == null || token.isBlank()) return false;
    return repository.existsByToken(token);
}
}
