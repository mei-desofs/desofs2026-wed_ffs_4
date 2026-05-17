package com.desofs.auth;

import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desofs.security.JwtUtil;
import com.desofs.security.TokenBlacklistService;
import com.desofs.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void registerShouldReturnCreatedUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        when(authService.register("user@example.com", "password123")).thenReturn(user);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }


    @Test
    void loginShouldReturnJwtAndRefreshToken() throws Exception {
        when(authService.login("user@example.com", "password123")).thenReturn(Map.of("token", "jwt-token", "refreshToken", "refresh-token"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void refreshShouldReturnNewTokens() throws Exception {
        when(authService.refresh("refresh-token")).thenReturn(Map.of("token", "new-jwt", "refreshToken", "new-refresh"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void loginShouldReturnUnauthorizedOnInvalidCredentials() throws Exception {
        when(authService.login("user@example.com", "wrong-password")).thenThrow(new IllegalArgumentException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void logoutShouldBlacklistToken() throws Exception {
        @SuppressWarnings("unchecked")
        Jws<Claims> jws = mock(Jws.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        Claims claims = mock(Claims.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        java.util.Date exp = new java.util.Date(System.currentTimeMillis() + 10000);

        when(jwtUtil.validate("jwt-token")).thenReturn(jws);
        when(jws.getBody()).thenReturn(claims);
        when(claims.getExpiration()).thenReturn(exp);
        doNothing().when(tokenBlacklistService).blacklist(eq("jwt-token"), any());

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out"));
    }
}
