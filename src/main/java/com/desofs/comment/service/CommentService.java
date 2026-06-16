package com.desofs.comment.service;

import com.desofs.comment.dto.CommentResponse;
import com.desofs.comment.dto.CreateCommentRequest;
import com.desofs.comment.dto.UpdateCommentRequest;
import com.desofs.comment.model.Comment;
import com.desofs.comment.repository.CommentRepository;
import com.desofs.project.repository.ProjectRepository;
import com.desofs.task.model.Task;
import com.desofs.task.repository.TaskRepository;
import com.desofs.user.model.User;
import com.desofs.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository     userRepository;
    private final TaskRepository     taskRepository;
    private final ProjectRepository  projectRepository;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    public CommentService(CommentRepository commentRepository,
                          UserRepository userRepository,
                          TaskRepository taskRepository,
                          ProjectRepository projectRepository) {
        this.commentRepository = commentRepository;
        this.userRepository    = userRepository;
        this.taskRepository    = taskRepository;
        this.projectRepository = projectRepository;
    }

    // ── FR-16: Create comment ─────────────────────────────────────────────────

    public CommentResponse addComment(UUID taskId, String userEmail, CreateCommentRequest request) {
        validateContent(request.getContent());

        // Verify the task exists (and is not soft-deleted) before attaching a comment
        Task task = resolveTask(taskId);

        User user = resolveUser(userEmail);

        // Project-scoped membership check: caller must be a member of the task's project
        verifyCallerProjectAccess(task.getProjectId(), user);

        Comment comment = new Comment(taskId, request.getContent().trim(), user.getId());
        return CommentResponse.from(commentRepository.save(comment));
    }

    // ── FR-16: List comments ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsForTask(UUID taskId, String userEmail) {
        Task task = resolveTask(taskId);

        User user = resolveUser(userEmail);
        verifyCallerProjectAccess(task.getProjectId(), user);

        return commentRepository.findByTaskId(taskId)
                .stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    // ── FR-17: Update comment ─────────────────────────────────────────────────

    public CommentResponse editComment(UUID taskId, Long commentId,
                                       String userEmail, UpdateCommentRequest request) {
        validateContent(request.getContent());

        // Verify task exists and caller is a project member before touching the comment
        Task task = resolveTask(taskId);
        User user = resolveUser(userEmail);
        verifyCallerProjectAccess(task.getProjectId(), user);

        Comment comment = commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        // Verify the comment actually belongs to the task in the URL (prevents cross-task edits)
        if (!comment.getTaskId().equals(taskId)) {
            throw new IllegalArgumentException(
                    "Comment " + commentId + " does not belong to task " + taskId);
        }

        // RBAC: only the author, or a manager/admin who is a member of *this* project, may edit
        if (!comment.getAuthorId().equals(user.getId()) && !isManagerOrAdmin(user)) {
            throw new AccessDeniedException("Forbidden: You can only edit your own comments");
        }

        comment.setContent(request.getContent().trim());
        comment.setUpdatedAt(LocalDateTime.now());
        return CommentResponse.from(commentRepository.save(comment));
    }

    // ── FR-18: Soft-delete comment ────────────────────────────────────────────

    public void deleteComment(UUID taskId, Long commentId, String userEmail) {
        Task task = resolveTask(taskId);
        User user = resolveUser(userEmail);
        verifyCallerProjectAccess(task.getProjectId(), user);

        Comment comment = commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        // Verify the comment actually belongs to the task in the URL
        if (!comment.getTaskId().equals(taskId)) {
            throw new IllegalArgumentException(
                    "Comment " + commentId + " does not belong to task " + taskId);
        }

        // RBAC: only the author, or a manager/admin who is a member of *this* project, may delete
        if (!comment.getAuthorId().equals(user.getId()) && !isManagerOrAdmin(user)) {
            throw new AccessDeniedException("Forbidden: You can only delete your own comments");
        }

        // Soft delete: preserve the row (SR-9)
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Loads the task and rejects soft-deleted or missing tasks so that comments
     * cannot be created/listed/edited against phantom task IDs.
     */
    private Task resolveTask(UUID taskId) {
        return taskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    /**
     * Mirrors TaskService.verifyCallerProjectAccess:
     * ADMIN bypasses membership; every other role must be a project member.
     */
    private void verifyCallerProjectAccess(Long projectId, User caller) {
        if ("ADMIN".equals(caller.getRole())) {
            return;
        }
        if (!projectRepository.isUserProjectMember(projectId, caller.getId())) {
            throw new AccessDeniedException("Caller is not a member of project: " + projectId);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content must not be blank");
        }
        if (content.trim().length() > 5000) {
            throw new IllegalArgumentException("Content must not exceed 5000 characters");
        }
    }

    private boolean isManagerOrAdmin(User user) {
        String role = user.getRole();
        return "MANAGER".equals(role) || "ADMIN".equals(role);
    }

    public java.util.Set<String> extractMentions(String text) {
        java.util.Set<String> mentions = new java.util.HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }
}