package com.desofs.comment;

import com.desofs.comment.dto.CommentResponse;
import com.desofs.comment.dto.CreateCommentRequest;
import com.desofs.comment.dto.UpdateCommentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentController")
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    @InjectMocks
    private CommentController controller;

    private static final String USER_EMAIL = "user@example.com";
    private static final Long TASK_ID = 42L;
    private static final Long COMMENT_ID = 7L;

    @BeforeEach
    void setUpSecurityContext() {
        Authentication auth = new UsernamePasswordAuthenticationToken(USER_EMAIL, null, List.of());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── POST /api/tasks/{taskId}/comments ─────────────────────────────────────

    @Nested
    @DisplayName("addComment")
    class AddComment {

        @Test
        @DisplayName("returns 201 and body on success")
        void addComment_success_returns201() {
            CommentResponse stub = buildResponse(COMMENT_ID, TASK_ID, "Hello");
            when(commentService.addComment(eq(TASK_ID), eq(USER_EMAIL), any(CreateCommentRequest.class)))
                    .thenReturn(stub);

            ResponseEntity<?> response = controller.addComment(TASK_ID, createRequest("Hello"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(stub);
        }

        @Test
        @DisplayName("returns 400 with error body on IllegalArgumentException")
        void addComment_validationError_returns400() {
            when(commentService.addComment(any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Content must not be blank"));

            ResponseEntity<?> response = controller.addComment(TASK_ID, createRequest(""));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorMessage(response)).contains("Content must not be blank");
        }

        @Test
        @DisplayName("delegates taskId and email to service correctly")
        void addComment_passesThroughTaskIdAndEmail() {
            when(commentService.addComment(any(), any(), any())).thenReturn(buildResponse(1L, TASK_ID, "x"));

            controller.addComment(TASK_ID, createRequest("x"));

            verify(commentService).addComment(eq(TASK_ID), eq(USER_EMAIL), any());
        }
    }

    // ── GET /api/tasks/{taskId}/comments ──────────────────────────────────────

    @Nested
    @DisplayName("getComments")
    class GetComments {

        @Test
        @DisplayName("returns 200 and list on success")
        void getComments_success_returns200() {
            List<CommentResponse> stub = List.of(
                    buildResponse(1L, TASK_ID, "First"),
                    buildResponse(2L, TASK_ID, "Second")
            );
            when(commentService.getCommentsForTask(TASK_ID)).thenReturn(stub);

            ResponseEntity<?> response = controller.getComments(TASK_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(stub);
        }

        @Test
        @DisplayName("returns 200 with empty list when no comments")
        void getComments_empty_returns200() {
            when(commentService.getCommentsForTask(TASK_ID)).thenReturn(List.of());

            ResponseEntity<?> response = controller.getComments(TASK_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat((List<?>) response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("returns 400 on IllegalArgumentException")
        void getComments_error_returns400() {
            when(commentService.getCommentsForTask(TASK_ID))
                    .thenThrow(new IllegalArgumentException("Task not found"));

            ResponseEntity<?> response = controller.getComments(TASK_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── PUT /api/tasks/{taskId}/comments/{commentId} ──────────────────────────

    @Nested
    @DisplayName("editComment")
    class EditComment {

        @Test
        @DisplayName("returns 200 and updated body on success")
        void editComment_success_returns200() {
            CommentResponse stub = buildResponse(COMMENT_ID, TASK_ID, "Updated");
            when(commentService.editComment(eq(COMMENT_ID), eq(USER_EMAIL), any(UpdateCommentRequest.class)))
                    .thenReturn(stub);

            ResponseEntity<?> response = controller.editComment(TASK_ID, COMMENT_ID, updateRequest("Updated"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(stub);
        }

        @Test
        @DisplayName("returns 400 with error body on forbidden")
        void editComment_forbidden_returns400() {
            when(commentService.editComment(any(), any(), any()))
                    .thenThrow(new IllegalArgumentException("Forbidden: You can only edit your own comments"));

            ResponseEntity<?> response = controller.editComment(TASK_ID, COMMENT_ID, updateRequest("Attempt"));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorMessage(response)).contains("Forbidden");
        }

        @Test
        @DisplayName("passes commentId (not taskId) to service")
        void editComment_passesCommentIdToService() {
            when(commentService.editComment(any(), any(), any())).thenReturn(buildResponse(COMMENT_ID, TASK_ID, "x"));

            controller.editComment(TASK_ID, COMMENT_ID, updateRequest("x"));

            verify(commentService).editComment(eq(COMMENT_ID), eq(USER_EMAIL), any());
        }
    }

    // ── DELETE /api/tasks/{taskId}/comments/{commentId} ───────────────────────

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("returns 204 on success")
        void deleteComment_success_returns204() {
            doNothing().when(commentService).deleteComment(COMMENT_ID, USER_EMAIL);

            ResponseEntity<?> response = controller.deleteComment(TASK_ID, COMMENT_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("returns 400 on forbidden")
        void deleteComment_forbidden_returns400() {
            doThrow(new IllegalArgumentException("Forbidden: You can only delete your own comments"))
                    .when(commentService).deleteComment(COMMENT_ID, USER_EMAIL);

            ResponseEntity<?> response = controller.deleteComment(TASK_ID, COMMENT_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(errorMessage(response)).contains("Forbidden");
        }

        @Test
        @DisplayName("returns 400 when comment not found")
        void deleteComment_notFound_returns400() {
            doThrow(new IllegalArgumentException("Comment not found: " + COMMENT_ID))
                    .when(commentService).deleteComment(COMMENT_ID, USER_EMAIL);

            ResponseEntity<?> response = controller.deleteComment(TASK_ID, COMMENT_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── Security context edge cases ───────────────────────────────────────────

    @Nested
    @DisplayName("currentUserEmail (security context)")
    class CurrentUserEmail {

        @Test
        @DisplayName("throws IllegalStateException when no authentication present")
        void noAuth_throwsIllegalState() {
            SecurityContextHolder.clearContext();

            // The controller throws directly in currentUserEmail() before the service is called,
            // so no stub is needed — and IllegalStateException is NOT caught by the 400 handler.
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> controller.addComment(TASK_ID, createRequest("Hello"))
            );
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CommentResponse buildResponse(Long id, Long taskId, String content) {
        // Use reflection-free approach: build via the Comment entity factory
        Comment c = new Comment(taskId, content, 1L);
        c.setId(id);
        c.setCreatedAt(LocalDateTime.now());
        return CommentResponse.from(c);
    }

    private CreateCommentRequest createRequest(String content) {
        CreateCommentRequest r = new CreateCommentRequest();
        r.setContent(content);
        return r;
    }

    private UpdateCommentRequest updateRequest(String content) {
        UpdateCommentRequest r = new UpdateCommentRequest();
        r.setContent(content);
        return r;
    }

    @SuppressWarnings("unchecked")
    private String errorMessage(ResponseEntity<?> response) {
        return ((Map<String, String>) response.getBody()).get("error");
    }
}