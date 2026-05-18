package com.desofs.comment;

import com.desofs.comment.dto.CreateCommentRequest;
import com.desofs.comment.dto.UpdateCommentRequest;
import com.desofs.user.User;
import com.desofs.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    private static final String USER_EMAIL = "test@example.com";
    private static final Long TASK_ID = 1L;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
    }

    private User ensureUserExists(String email) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setPassword("password123");
            user.setRole("MEMBER");
            return userRepository.save(user);
        });
    }

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "MEMBER")
    @DisplayName("POST creates comment and returns 201")
    void createComment_success() throws Exception {
        ensureUserExists(USER_EMAIL);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("Hello world");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("Hello world"))
                .andExpect(jsonPath("$.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.authorId").exists());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "MEMBER")
    @DisplayName("POST with empty content returns 400")
    void createComment_emptyContent_returns400() throws Exception {
        ensureUserExists(USER_EMAIL);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "MEMBER")
    @DisplayName("POST with XSS content escapes output")
    void createComment_xssContent_escapesHtml() throws Exception {
        ensureUserExists(USER_EMAIL);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent("<script>alert(1)</script>");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "MEMBER")
    @DisplayName("GET returns list of comments")
    void getComments_returnsList() throws Exception {
        User user = ensureUserExists(USER_EMAIL);
        Comment comment = new Comment(TASK_ID, "Test comment", user.getId());
        commentRepository.save(comment);

        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].content").value("Test comment"));
    }

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "MEMBER")
    @DisplayName("PUT updates comment and returns 200")
    void updateComment_success() throws Exception {
        User user = ensureUserExists(USER_EMAIL);
        Comment comment = new Comment(TASK_ID, "Original", user.getId());
        comment = commentRepository.save(comment);

        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Updated content");

        mockMvc.perform(put("/api/tasks/{taskId}/comments/{commentId}", TASK_ID, comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @WithMockUser(username = USER_EMAIL, roles = "MEMBER")
    @DisplayName("DELETE soft-deletes comment and returns 204")
    void deleteComment_success() throws Exception {
        User user = ensureUserExists(USER_EMAIL);
        Comment comment = new Comment(TASK_ID, "To delete", user.getId());
        comment = commentRepository.save(comment);

        mockMvc.perform(delete("/api/tasks/{taskId}/comments/{commentId}", TASK_ID, comment.getId()))
                .andExpect(status().isNoContent());

        Comment deleted = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    @WithMockUser(username = "other@example.com", roles = "MEMBER")
    @DisplayName("PUT on others comment returns 403")
    void updateComment_otherUser_returns403() throws Exception {
        User otherUser = ensureUserExists("other@example.com");
        User originalUser = ensureUserExists(USER_EMAIL);

        Comment comment = new Comment(TASK_ID, "Original", originalUser.getId());
        comment = commentRepository.save(comment);

        UpdateCommentRequest request = new UpdateCommentRequest();
        request.setContent("Hacked");

        mockMvc.perform(put("/api/tasks/{taskId}/comments/{commentId}", TASK_ID, comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET without auth returns 403")
    void getComments_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskId}/comments", TASK_ID))
                .andExpect(status().isForbidden());
    }
}