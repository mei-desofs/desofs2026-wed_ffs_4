package com.desofs.e2e;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.desofs.user.User;
import com.desofs.user.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApplicationE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void seedAdminUserIfMissing() {
        ensureAdminUser();
    }

    @Test
    void adminCanLoginCreateProjectAndCreateTaskEndToEnd() {
        ensureAdminUser();
        String token = login("admin@example.com", "password123");
        assertThat(token).isNotBlank();

        Map<String, Object> project = createProject(token, "E2E Project", "Created by end-to-end test");
        assertThat(project.get("id")).isNotNull();

        Number projectId = (Number) project.get("id");
        Map<String, Object> task = createTask(token, projectId.longValue(), "E2E Task");
        assertThat(task).isNotNull();
        assertThat(task.get("id")).isNotNull();
        assertThat(task.get("title")).isEqualTo("E2E Task");
        assertThat(task.get("status")).isEqualTo("TODO");

        ResponseEntity<Map<String, Object>> projectResponse = exchangeMap(
                url("/api/projects/%d".formatted(projectId.longValue())),
                HttpMethod.GET,
            authedEntity(token, null));
        assertThat(projectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> projectBody = requireBody(projectResponse);
        assertThat(projectBody.get("name")).isEqualTo("E2E Project");
    }

    @Test
    void invalidCredentialsAreRejectedEndToEnd() {
        ensureAdminUser();
        ResponseEntity<Map<String, Object>> response = exchangeMap(
                url("/auth/login"),
                HttpMethod.POST,
            jsonEntity(Map.of("email", "admin@example.com", "password", "wrong")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Map<String, Object> body = requireBody(response);
        assertThat(body.get("error")).isEqualTo("Invalid credentials");
    }

    private String login(String email, String password) {
        ResponseEntity<Map<String, Object>> response = exchangeMap(
                url("/auth/login"),
                HttpMethod.POST,
            jsonEntity(Map.of("email", email, "password", password)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = requireBody(response);
        return (String) body.get("token");
    }

    private Map<String, Object> createProject(String token, String name, String description) {
        ResponseEntity<Map<String, Object>> response = exchangeMap(
                url("/api/projects"),
                HttpMethod.POST,
            authedEntity(token, Map.of("name", name, "description", description)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return requireBody(response);
    }

    private Map<String, Object> createTask(String token, long projectId, String title) {
        ResponseEntity<Map<String, Object>> response = exchangeMap(
                url("/api/projects/%d/tasks".formatted(projectId)),
                HttpMethod.POST,
            authedEntity(token, Map.of("title", title, "description", "Created by end-to-end test", "priority", "HIGH")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return requireBody(response);
    }

    private HttpEntity<Object> authedEntity(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Object> jsonEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private ResponseEntity<Map<String, Object>> exchangeMap(String path, HttpMethod method, HttpEntity<Object> entity) {
        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.exchange(
                path,
                method,
                entity,
                Map.class);
        return response;
    }

    private Map<String, Object> requireBody(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private void ensureAdminUser() {
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
    }
}