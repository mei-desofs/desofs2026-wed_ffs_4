package com.desofs.comment;

import com.desofs.comment.dto.CommentResponse;
import com.desofs.comment.dto.CreateCommentRequest;
import com.desofs.comment.dto.UpdateCommentRequest;
import com.desofs.project.repository.ProjectRepository;
import com.desofs.task.Task;
import com.desofs.task.TaskRepository;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private UserRepository    userRepository;
    @Mock private TaskRepository    taskRepository;       // new dependency
    @Mock private ProjectRepository projectRepository;   // new dependency

    @InjectMocks
    private CommentService commentService;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final UUID TASK_UUID       = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TASK_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Long PROJECT_ID      = 99L;

    private User    author;
    private User    manager;
    private User    admin;
    private User    otherUser;
    private Comment existingComment;
    private Task    testTask;

    @BeforeEach
    void setUp() {
        author    = buildUser(1L, "author@example.com",  "USER");
        manager   = buildUser(2L, "manager@example.com", "MANAGER");
        admin     = buildUser(3L, "admin@example.com",   "ADMIN");
        otherUser = buildUser(4L, "other@example.com",   "USER");

        // existingComment lives on TASK_UUID
        existingComment = buildComment(10L, TASK_UUID, "Original content", author.getId());

        // Task returned by resolveTask()
        testTask = new Task();
        testTask.setProjectId(PROJECT_ID);
        testTask.setTitle("Test Task");
        testTask.setCreatedBy(author.getId());
    }

    // ── Helpers: stub resolveTask + verifyCallerProjectAccess ─────────────────

    /**
     * Stubs the two repository calls that every mutating service method now makes
     * before touching comments:
     *   1. taskRepository.findByIdAndDeletedFalse  →  the test task
     *   2. projectRepository.isUserProjectMember   →  true
     *      (skipped for ADMIN, who bypasses the membership check)
     */
    private void stubTaskAccess(UUID taskId, User caller) {
        when(taskRepository.findByIdAndDeletedFalse(taskId)).thenReturn(Optional.of(testTask));
        if (!"ADMIN".equals(caller.getRole())) {
            when(projectRepository.isUserProjectMember(PROJECT_ID, caller.getId())).thenReturn(true);
        }
    }

    // ── FR-16: addComment ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("addComment")
    class AddComment {

        @Test
        @DisplayName("saves and returns comment when request is valid")
        void addComment_validRequest_returnsResponse() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);

            CommentResponse response = commentService.addComment(TASK_UUID, author.getEmail(), createRequest("Hello world"));

            assertThat(response).isNotNull();
            assertThat(response.getTaskId()).isEqualTo(TASK_UUID);
            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("trims whitespace from content before saving")
        void addComment_trimsContent() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

            commentService.addComment(TASK_UUID, author.getEmail(), createRequest("  trimmed  "));

            verify(commentRepository).save(argThat(c -> "trimmed".equals(c.getContent())));
        }

        @Test
        @DisplayName("throws when content is null")
        void addComment_nullContent_throws() {
            // validateContent fires before resolveTask — no task stub needed
            assertThatThrownBy(() -> commentService.addComment(TASK_UUID, author.getEmail(), createRequest(null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("throws when content is blank")
        void addComment_blankContent_throws() {
            assertThatThrownBy(() -> commentService.addComment(TASK_UUID, author.getEmail(), createRequest("   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("throws when content exceeds 5000 characters")
        void addComment_contentTooLong_throws() {
            assertThatThrownBy(() -> commentService.addComment(TASK_UUID, author.getEmail(), createRequest("x".repeat(5001))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5000");
        }

        @Test
        @DisplayName("accepts content of exactly 5000 characters")
        void addComment_contentAtLimit_succeeds() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);

            assertThatCode(() -> commentService.addComment(TASK_UUID, author.getEmail(), createRequest("x".repeat(5000))))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws when task does not exist")
        void addComment_taskNotFound_throws() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.addComment(TASK_UUID, author.getEmail(), createRequest("Valid")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Task not found");
        }

        @Test
        @DisplayName("throws AccessDeniedException when caller is not a project member")
        void addComment_notProjectMember_throws() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_UUID)).thenReturn(Optional.of(testTask));
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(projectRepository.isUserProjectMember(PROJECT_ID, author.getId())).thenReturn(false);

            assertThatThrownBy(() -> commentService.addComment(TASK_UUID, author.getEmail(), createRequest("Valid")))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("throws when user is not found")
        void addComment_unknownUser_throws() {
            // resolveTask succeeds, resolveUser fails
            when(taskRepository.findByIdAndDeletedFalse(TASK_UUID)).thenReturn(Optional.of(testTask));
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.addComment(TASK_UUID, "ghost@example.com", createRequest("Valid content")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ghost@example.com");
        }

        @Test
        @DisplayName("escapes HTML in content to prevent XSS (SR-7)")
        void addComment_escapesHtmlContent() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            Comment saved = buildComment(1L, TASK_UUID, "<script>alert(1)</script>", author.getId());
            when(commentRepository.save(any())).thenReturn(saved);

            CommentResponse response = commentService.addComment(TASK_UUID, author.getEmail(), createRequest("<script>alert(1)</script>"));

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
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            Comment second = buildComment(11L, TASK_UUID, "Second comment", author.getId());
            when(commentRepository.findByTaskId(TASK_UUID)).thenReturn(List.of(existingComment, second));

            List<CommentResponse> result = commentService.getCommentsForTask(TASK_UUID, author.getEmail());

            assertThat(result).hasSize(2);
            verify(commentRepository).findByTaskId(TASK_UUID);
        }

        @Test
        @DisplayName("returns empty list when task has no comments")
        void getCommentsForTask_noComments_returnsEmptyList() {
            stubTaskAccess(OTHER_TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findByTaskId(OTHER_TASK_UUID)).thenReturn(List.of());

            List<CommentResponse> result = commentService.getCommentsForTask(OTHER_TASK_UUID, author.getEmail());

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
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            CommentResponse response = commentService.editComment(TASK_UUID, 10L, author.getEmail(), updateRequest("Updated content"));

            assertThat(response).isNotNull();
            verify(commentRepository).save(existingComment);
        }

        @Test
        @DisplayName("manager can edit any comment in their project")
        void editComment_byManager_succeeds() {
            stubTaskAccess(TASK_UUID, manager);
            when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            assertThatCode(() -> commentService.editComment(TASK_UUID, 10L, manager.getEmail(), updateRequest("Manager edit")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("admin can edit any comment")
        void editComment_byAdmin_succeeds() {
            // ADMIN bypasses project membership check → only task stub needed
            when(taskRepository.findByIdAndDeletedFalse(TASK_UUID)).thenReturn(Optional.of(testTask));
            when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            assertThatCode(() -> commentService.editComment(TASK_UUID, 10L, admin.getEmail(), updateRequest("Admin edit")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("non-author regular user cannot edit someone else's comment")
        void editComment_byOtherUser_throwsForbidden() {
            // otherUser is a project member but not the comment author and not a manager/admin
            stubTaskAccess(TASK_UUID, otherUser);
            when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));

            assertThatThrownBy(() -> commentService.editComment(TASK_UUID, 10L, otherUser.getEmail(), updateRequest("Sneaky edit")))
                    .isInstanceOf(AccessDeniedException.class)   // was SecurityException
                    .hasMessageContaining("Forbidden");
        }

        @Test
        @DisplayName("throws when comment does not belong to the given task")
        void editComment_wrongTask_throws() {
            // existingComment.taskId == TASK_UUID, but we pass OTHER_TASK_UUID
            stubTaskAccess(OTHER_TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));

            assertThatThrownBy(() -> commentService.editComment(OTHER_TASK_UUID, 10L, author.getEmail(), updateRequest("x")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to task");
        }

        @Test
        @DisplayName("throws when comment not found")
        void editComment_commentNotFound_throws() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.editComment(TASK_UUID, 999L, author.getEmail(), updateRequest("Something")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("throws when new content is blank")
        void editComment_blankContent_throws() {
            // validateContent fires before resolveTask — no task stub needed
            assertThatThrownBy(() -> commentService.editComment(TASK_UUID, 10L, author.getEmail(), updateRequest("   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("sets updatedAt on successful edit")
        void editComment_setsUpdatedAt() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            commentService.editComment(TASK_UUID, 10L, author.getEmail(), updateRequest("New content"));

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
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            commentService.deleteComment(TASK_UUID, 10L, author.getEmail());

            assertThat(existingComment.getDeletedAt()).isNotNull();
            verify(commentRepository).save(existingComment);
        }

        @Test
        @DisplayName("manager can soft-delete any comment in their project")
        void deleteComment_byManager_succeeds() {
            stubTaskAccess(TASK_UUID, manager);
            when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            assertThatCode(() -> commentService.deleteComment(TASK_UUID, 10L, manager.getEmail()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("admin can soft-delete any comment")
        void deleteComment_byAdmin_succeeds() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_UUID)).thenReturn(Optional.of(testTask));
            when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            assertThatCode(() -> commentService.deleteComment(TASK_UUID, 10L, admin.getEmail()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("non-author regular user cannot delete someone else's comment")
        void deleteComment_byOtherUser_throwsForbidden() {
            stubTaskAccess(TASK_UUID, otherUser);
            when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));

            assertThatThrownBy(() -> commentService.deleteComment(TASK_UUID, 10L, otherUser.getEmail()))
                    .isInstanceOf(AccessDeniedException.class)   // was SecurityException
                    .hasMessageContaining("Forbidden");
        }

        @Test
        @DisplayName("throws when comment does not belong to the given task")
        void deleteComment_wrongTask_throws() {
            stubTaskAccess(OTHER_TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));

            assertThatThrownBy(() -> commentService.deleteComment(OTHER_TASK_UUID, 10L, author.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to task");
        }

        @Test
        @DisplayName("throws when comment not found")
        void deleteComment_commentNotFound_throws() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteComment(TASK_UUID, 999L, author.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("999");
        }

        @Test
        @DisplayName("does not hard-delete: row is still saved after delete")
        void deleteComment_doesNotRemoveRow() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            commentService.deleteComment(TASK_UUID, 10L, author.getEmail());

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
        @DisplayName("documents that email domain part is not captured as a mention")
        void extractMentions_emailAddress_ignoresDomain() {
            Set<String> mentions = commentService.extractMentions("contact user@example.com for help");
            assertThat(mentions).doesNotContain("com");
        }
    }


    @Nested
    class EdgeCasesAndBranches {

        @Test
        @DisplayName("validateContent accepts exactly 5000 character content")
        void validateContent_maxLength_succeeds() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);

            String maxContent = "x".repeat(5000);
            assertThatCode(() -> commentService.addComment(TASK_UUID, author.getEmail(), createRequest(maxContent)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isManagerOrAdmin returns true for MANAGER role")
        void userWithManagerRole_isManagerOrAdmin_true() {
            stubTaskAccess(TASK_UUID, manager);
            when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            assertThatCode(() -> commentService.editComment(TASK_UUID, 10L, manager.getEmail(), updateRequest("Manager edit")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("isManagerOrAdmin returns true for ADMIN role")
        void userWithAdminRole_isManagerOrAdmin_true() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_UUID)).thenReturn(Optional.of(testTask));
            when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            assertThatCode(() -> commentService.editComment(TASK_UUID, 10L, admin.getEmail(), updateRequest("Admin edit")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deleteComment with admin user succeeds")
        void deleteComment_byAdmin_succeeds() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_UUID)).thenReturn(Optional.of(testTask));
            when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            assertThatCode(() -> commentService.deleteComment(TASK_UUID, 10L, admin.getEmail()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("verifyCallerProjectAccess bypasses check for ADMIN")
        void adminBypassesProjectMembershipCheck() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_UUID)).thenReturn(Optional.of(testTask));
            when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
            when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);

            CommentResponse response = commentService.addComment(TASK_UUID, admin.getEmail(), createRequest("Admin comment"));

            assertThat(response).isNotNull();
            verify(projectRepository, never()).isUserProjectMember(PROJECT_ID, admin.getId());
        }

        @Test
        @DisplayName("extractMentions with complex mentions")
        void extractMentions_complexPatterns() {
            Set<String> mentions = commentService.extractMentions("@alice123 @bob_smith @user-test");
            assertThat(mentions).contains("alice123", "bob_smith", "user");
        }

        @Test
        @DisplayName("extractMentions ignores invalid mention patterns")
        void extractMentions_ignoresSymbols() {
            Set<String> mentions = commentService.extractMentions("@#$$ @@@@ @@user");
            assertThat(mentions).doesNotContain("#$$", "@@");
        }

        @Test
        @DisplayName("validateContent with content of exactly 1 character succeeds")
        void validateContent_minLength_succeeds() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.save(any(Comment.class))).thenReturn(existingComment);

            assertThatCode(() -> commentService.addComment(TASK_UUID, author.getEmail(), createRequest("x")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("getCommentsForTask checks project membership for non-admin")
        void getCommentsForTask_verifies_projectAccess() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            when(commentRepository.findByTaskId(TASK_UUID)).thenReturn(List.of(existingComment));

            commentService.getCommentsForTask(TASK_UUID, author.getEmail());

            verify(projectRepository).isUserProjectMember(PROJECT_ID, author.getId());
        }

        @Test
        @DisplayName("editComment throws when comment found but belongs to different task")
        void editComment_wrongTaskId_throwsIllegalArgument() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            Comment differentTaskComment = buildComment(10L, OTHER_TASK_UUID, "Original", author.getId());
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(differentTaskComment));

            assertThatThrownBy(() -> commentService.editComment(TASK_UUID, 10L, author.getEmail(), updateRequest("New")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to task");
        }

        @Test
        @DisplayName("deleteComment throws when comment belongs to different task")
        void deleteComment_wrongTaskId_throwsIllegalArgument() {
            stubTaskAccess(TASK_UUID, author);
            when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
            Comment differentTaskComment = buildComment(10L, OTHER_TASK_UUID, "Original", author.getId());
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(differentTaskComment));

            assertThatThrownBy(() -> commentService.deleteComment(TASK_UUID, 10L, author.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not belong to task");
        }

        @Test
        @DisplayName("resolveTask throws with proper task not found message")
        void resolveTask_notFound_throwsWithTaskId() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.addComment(TASK_UUID, author.getEmail(), createRequest("Valid")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Task not found")
                    .hasMessageContaining(TASK_UUID.toString());
        }

        @Test
        @DisplayName("resolveUser throws when user not found in database")
        void resolveUser_notFound_throwsWithEmail() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_UUID)).thenReturn(Optional.of(testTask));
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.addComment(TASK_UUID, "unknown@example.com", createRequest("Valid")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("unknown@example.com");
        }

        @Test
        @DisplayName("editComment with manager role on non-author comment succeeds")
        void editComment_byManagerOnOthersComment_succeeds() {
            stubTaskAccess(TASK_UUID, manager);
            when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            CommentResponse response = commentService.editComment(TASK_UUID, 10L, manager.getEmail(), updateRequest("Manager modified"));

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("deleteComment with manager role on non-author comment succeeds")
        void deleteComment_byManagerOnOthersComment_succeeds() {
            stubTaskAccess(TASK_UUID, manager);
            when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
            when(commentRepository.findActiveById(10L)).thenReturn(Optional.of(existingComment));
            when(commentRepository.save(any())).thenReturn(existingComment);

            assertThatCode(() -> commentService.deleteComment(TASK_UUID, 10L, manager.getEmail()))
                    .doesNotThrowAnyException();
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

    private Comment buildComment(Long id, UUID taskId, String content, Long authorId) {
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