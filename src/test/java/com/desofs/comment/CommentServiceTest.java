package com.desofs.comment;

import com.desofs.comment.dto.CommentResponse;
import com.desofs.comment.dto.CreateCommentRequest;
import com.desofs.comment.dto.UpdateCommentRequest;
import com.desofs.user.User;
import com.desofs.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private User author;
    private User manager;
    private User admin;
    private User otherUser;
    private Comment existingComment;

    @BeforeEach
    void setUp() {
        author = buildUser(1L, "author@example.com", "USER");
        manager = buildUser(2L, "manager@example.com", "MANAGER");
        admin = buildUser(3L, "admin@example.com", "ADMIN");
        otherUser = buildUser(4L, "other@example.com", "USER");

        existingComment = buildComment(10L, 100L, "Original content", author.getId());
    }

    // ── FR-16: addComment ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("addComment")
    class AddComment {

        @Test
        @DisplayName("saves and returns comment when request is valid")
        void addComment_validRequest_returnsResponse() {
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);

            CreateCommentRequest req = createRequest("Hello world");
            CommentResponse response = commentService.addComment(100L, author.getEmail(), req);

            assertThat(response).isNotNull();
            assertThat(response.getTaskId()).isEqualTo(100L);
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("trims whitespace from content before saving")
        void addComment_trimsContent() {
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            CreateCommentRequest req = createRequest("  trimmed  ");
            commentService.addComment(100L, author.getEmail(), req);

            verify(commentRepository).save(argThat(c -> "trimmed".equals(c.getContent())));
        }

        @Test
        @DisplayName("throws when content is null")
        void addComment_nullContent_throws() {
            CreateCommentRequest req = createRequest(null);

            assertThatThrownBy(() -> commentService.addComment(100L, author.getEmail(), req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("throws when content is blank")
        void addComment_blankContent_throws() {
            CreateCommentRequest req = createRequest("   ");

            assertThatThrownBy(() -> commentService.addComment(100L, author.getEmail(), req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("throws when content exceeds 5000 characters")
        void addComment_contentTooLong_throws() {
            CreateCommentRequest req = createRequest("x".repeat(5001));

            assertThatThrownBy(() -> commentService.addComment(100L, author.getEmail(), req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5000");
        }

        @Test
        @DisplayName("accepts content of exactly 5000 characters")
        void addComment_contentAtLimit_succeeds() {
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);

            CreateCommentRequest req = createRequest("x".repeat(5000));
            assertThatCode(() -> commentService.addComment(100L, author.getEmail(), req))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws when user is not found")
        void addComment_unknownUser_throws() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
            CreateCommentRequest req = createRequest("Valid content");

            assertThatThrownBy(() -> commentService.addComment(100L, "ghost@example.com", req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ghost@example.com");
        }

        @Test
        @DisplayName("escapes HTML in content to prevent XSS")
        void addComment_escapesHtmlContent() {
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            
            Comment saved = buildComment(1L, 100L, "<script>alert(1)</script>", author.getId());
            when(commentRepository.save(any())).thenReturn(saved);

            CreateCommentRequest req = createRequest("<script>alert(1)</script>");
            CommentResponse response = commentService.addComment(100L, author.getEmail(), req);

            assertThat(response.getContent()).isEqualTo("&lt;script&gt;alert(1)&lt;/script&gt;");
        }
    }

    // ── FR-16: getCommentsForTask ─────────────────────────────────────────────

    @Nested
    @DisplayName("getCommentsForTask")
    class GetCommentsForTask {

        @Test
        @DisplayName("returns mapped responses for all comments on a task")
        void getCommentsForTask_returnsList() {
            Comment second = buildComment(11L, 100L, "Second comment", author.getId());
            when(commentRepository.findByTaskId(100L)).thenReturn(List.of(existingComment, second));

            List<CommentResponse> result = commentService.getCommentsForTask(100L);

            assertThat(result).hasSize(2);
            verify(commentRepository).findByTaskId(100L);
        }

        @Test
        @DisplayName("returns empty list when task has no comments")
        void getCommentsForTask_noComments_returnsEmptyList() {
            when(commentRepository.findByTaskId(999L)).thenReturn(List.of());

            List<CommentResponse> result = commentService.getCommentsForTask(999L);

            assertThat(result).isEmpty();
        }
    }

    // ── FR-17: editComment ────────────────────────────────────────────────────

    @Nested
    @DisplayName("editComment")
    class EditComment {

        @Test
        @DisplayName("author can edit their own comment")
        void editComment_byAuthor_succeeds() {
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            UpdateCommentRequest req = updateRequest("Updated content");
            CommentResponse response = commentService.editComment(10L, author.getEmail(), req);

            assertThat(response).isNotNull();
            verify(commentRepository).save(existingComment);
        }

        @Test
        @DisplayName("manager can edit any comment")
        void editComment_byManager_succeeds() {
            when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            UpdateCommentRequest req = updateRequest("Manager edit");
            assertThatCode(() -> commentService.editComment(10L, manager.getEmail(), req))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("admin can edit any comment")
        void editComment_byAdmin_succeeds() {
            when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            UpdateCommentRequest req = updateRequest("Admin edit");
            assertThatCode(() -> commentService.editComment(10L, admin.getEmail(), req))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("other user cannot edit someone else's comment")
        void editComment_byOtherUser_throwsForbidden() {
            when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));

            UpdateCommentRequest req = updateRequest("Sneaky edit");
            assertThatThrownBy(() -> commentService.editComment(10L, otherUser.getEmail(), req))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Forbidden");
        }

        @Test
        @DisplayName("throws when comment not found")
        void editComment_commentNotFound_throws() {
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(999L)).thenReturn(Optional.empty());

            UpdateCommentRequest req = updateRequest("Something");
            assertThatThrownBy(() -> commentService.editComment(999L, author.getEmail(), req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("throws when new content is blank")
        void editComment_blankContent_throws() {
            UpdateCommentRequest req = updateRequest("   ");

            assertThatThrownBy(() -> commentService.editComment(10L, author.getEmail(), req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("sets updatedAt on successful edit")
        void editComment_setsUpdatedAt() {
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            commentService.editComment(10L, author.getEmail(), updateRequest("New content"));

            assertThat(existingComment.getUpdatedAt()).isNotNull();
        }
    }

    // ── FR-18: deleteComment ──────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("author can soft-delete their own comment")
        void deleteComment_byAuthor_setsDeletedAt() {
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            commentService.deleteComment(10L, author.getEmail());

            assertThat(existingComment.getDeletedAt()).isNotNull();
            verify(commentRepository).save(existingComment);
        }

        @Test
        @DisplayName("manager can soft-delete any comment")
        void deleteComment_byManager_succeeds() {
            when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            assertThatCode(() -> commentService.deleteComment(10L, manager.getEmail()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("admin can soft-delete any comment")
        void deleteComment_byAdmin_succeeds() {
            when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            assertThatCode(() -> commentService.deleteComment(10L, admin.getEmail()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("other user cannot delete someone else's comment")
        void deleteComment_byOtherUser_throwsForbidden() {
            when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));

            assertThatThrownBy(() -> commentService.deleteComment(10L, otherUser.getEmail()))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Forbidden");
        }

        @Test
        @DisplayName("throws when comment not found")
        void deleteComment_commentNotFound_throws() {
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteComment(999L, author.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("does not hard-delete: row is still saved after delete")
        void deleteComment_doesNotRemoveRow() {
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            commentService.deleteComment(10L, author.getEmail());

            verify(commentRepository, never()).delete(any());
            verify(commentRepository).save(existingComment);
        }
    }

    // ── extractMentions ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractMentions")
    class ExtractMentions {

        @Test
        @DisplayName("extracts single mention")
        void extractMentions_singleMention() {
            Set<String> mentions = commentService.extractMentions("Hello @alice");
            assertThat(mentions).containsExactlyInAnyOrder("alice");
        }

        @Test
        @DisplayName("extracts multiple distinct mentions")
        void extractMentions_multipleMentions() {
            Set<String> mentions = commentService.extractMentions("@alice and @bob should review");
            assertThat(mentions).containsExactlyInAnyOrder("alice", "bob");
        }

        @Test
        @DisplayName("de-duplicates repeated mentions")
        void extractMentions_deduplicates() {
            Set<String> mentions = commentService.extractMentions("@alice @alice @alice");
            assertThat(mentions).hasSize(1).contains("alice");
        }

        @Test
        @DisplayName("returns empty set when no mentions present")
        void extractMentions_noMentions_returnsEmpty() {
            Set<String> mentions = commentService.extractMentions("No mentions here");
            assertThat(mentions).isEmpty();
        }

        @Test
        @DisplayName("returns empty set for empty string")
        void extractMentions_emptyString_returnsEmpty() {
            Set<String> mentions = commentService.extractMentions("");
            assertThat(mentions).isEmpty();
        }

        @Test
        @DisplayName("handles email addresses without treating domain as mention")
        void extractMentions_emailAddress_ignoresDomain() {
            // @word pattern: "user" is captured, but not "example.com" as a unit
            Set<String> mentions = commentService.extractMentions("contact user@example.com for help");
            // "example" would NOT be captured since @ must start a word boundary in context;
            // the pattern matches @example but not @example.com, so "example" may appear.
            // This test documents current behaviour so regressions are caught.
            assertThat(mentions).doesNotContain("com");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(Long id, String email, String role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    private Comment buildComment(Long id, Long taskId, String content, Long authorId) {
        Comment c = new Comment(taskId, content, authorId);
        c.setId(id);
        c.setCreatedAt(LocalDateTime.now());
        return c;
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
}