package com.desofs.auth;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
        @org.junit.jupiter.api.BeforeEach
        void setUp() {
            refreshTokenService = new RefreshTokenService(refreshTokenRepository, 3600000L);
        }
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;


    @Test
    void createRefreshTokenShouldSaveAndReturnToken() {
        String email = "user@example.com";
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(refreshTokenRepository).deleteByUserEmail(email);
        RefreshToken token = refreshTokenService.createRefreshToken(email);
        assertNotNull(token.getToken());
        assertEquals(email, token.getUserEmail());
        assertTrue(token.getExpiryDate().isAfter(Instant.now()));
    }

    @Test
    void findByTokenShouldReturnTokenIfExists() {
        RefreshToken token = new RefreshToken("tok", "user@example.com", Instant.now().plusSeconds(1000));
        when(refreshTokenRepository.findByToken("tok")).thenReturn(Optional.of(token));
        Optional<RefreshToken> found = refreshTokenService.findByToken("tok");
        assertTrue(found.isPresent());
        assertEquals("tok", found.get().getToken());
    }

    @Test
    void deleteByUserEmailShouldCallRepository() {
        refreshTokenService.deleteByUserEmail("user@example.com");
        verify(refreshTokenRepository).deleteByUserEmail("user@example.com");
    }
}
