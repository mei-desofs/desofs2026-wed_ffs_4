package com.desofs.comment;

import com.desofs.comment.dto.CreateCommentRequest;
import com.desofs.comment.dto.UpdateCommentRequest;
import com.desofs.comment.model.Comment;
import com.desofs.comment.repository.CommentRepository;
import com.desofs.task.model.Task;
import com.desofs.task.repository.TaskRepository;
import com.desofs.user.model.User;
import com.desofs.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class CommentControllerIntegrationTest {

    @Autowired private MockMvc            mockMvc;
    @Autowired private ObjectMapper       objectMapper;
    @Autowired private CommentRepository commentRepository;
    @Autowired private UserRepository     userRepository;
    @Autowired private TaskRepository     taskRepository;   // needed to satisfy resolveTask()

    private static final String USER_EMAIL = "test@example.com";

    // Resolved in setUp — each test runs in the same @Transactional boundary
    private UUID taskId;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();

        // Ensure the test user exists so resolveUser() succeeds
        User admin = ensureUserExists(USER_EMAIL, "ADMIN");

        // Create a real Task so resolveTask() does not throw.
        // Using projectId=1L; no FK constraint is expected in the test DB.
        // ADMIN role in @WithMockUser bypasses verifyCallerProjectAccess.
        Task task = new Task();
        task.setProjectId(1L);
        task.setTitle("Integration Test Task");
        task.setCreatedBy(admin.getId());
        taskId = taskRepository.save(task).getId();
    }

    // ── FR-16: Create comment ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "ADMIN")
    @DisplayName("POST creates comment and returns 201")
    void createComment_success() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Hello world");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("Hello world"))
                .andExpect(jsonPath("$.taskId").value(taskId.toString()))  // UUID serialises as string
                .andExpect(jsonPath("$.authorId").exists());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "ADMIN")
    @DisplayName("POST with empty content returns 400")
    void createComment_emptyContent_returns400() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "ADMIN")
    @DisplayName("POST with XSS content escapes output (SR-7)")
    void createComment_xssContent_escapesHtml() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("<script>alert(1)</script>");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    // ── FR-16: List comments ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "ADMIN")
    @DisplayName("GET returns list of comments")
    void getComments_returnsList() throws Exception {
        User user = ensureUserExists(USER_EMAIL, "ADMIN");
        Comment comment = new Comment(taskId, "Test comment", user.getId());
        commentRepository.save(comment);

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].content").value("Test comment"));
    }

    // ── FR-17: Update comment ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "ADMIN")
    @DisplayName("PUT updates comment and returns 200")
    void updateComment_success() throws Exception {
        User user = ensureUserExists(USER_EMAIL, "ADMIN");
        Comment comment = new Comment(taskId, "Original", user.getId());
        comment = commentRepository.save(comment);

        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated content");

        mockMvc.perform(put("/api/tasks/{taskId}/comments/{commentId}", taskId, comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    // ── FR-18: Delete comment ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "ADMIN")
    @DisplayName("DELETE soft-deletes comment and returns 204")
    void deleteComment_success() throws Exception {
        User user = ensureUserExists(USER_EMAIL, "ADMIN");
        Comment comment = new Comment(taskId, "To delete", user.getId());
        comment = commentRepository.save(comment);

        mockMvc.perform(delete("/api/tasks/{taskId}/comments/{commentId}", taskId, comment.getId()))
                .andExpect(status().isNoContent());

        Comment deleted = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    // ── RBAC ──────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "other@example.com", roles = "MEMBER")
    @DisplayName("PUT on another user's comment returns 403 (project non-member)")
    void updateComment_otherUser_returns403() throws Exception {
        // other@example.com is a MEMBER who is not in any project → 403 at project-access check
        ensureUserExists("other@example.com", "MEMBER");
        User originalUser = ensureUserExists(USER_EMAIL, "ADMIN");

        Comment comment = new Comment(taskId, "Original", originalUser.getId());
        comment = commentRepository.save(comment);

        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Hacked");

        mockMvc.perform(put("/api/tasks/{taskId}/comments/{commentId}", taskId, comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ── Authentication ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET without auth returns 401/403")
    void getComments_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User ensureUserExists(String email, String role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setPassword("password123");
            user.setRole(role);
            return userRepository.save(user);
        });
    }
}