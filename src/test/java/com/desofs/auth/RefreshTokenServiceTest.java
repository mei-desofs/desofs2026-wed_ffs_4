package com.desofs.auth;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;


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
