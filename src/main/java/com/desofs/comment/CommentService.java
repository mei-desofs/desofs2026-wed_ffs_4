package com.desofs.comment;

import com.desofs.comment.dto.CommentResponse;
import com.desofs.comment.dto.CreateCommentRequest;
import com.desofs.comment.dto.UpdateCommentRequest;
import com.desofs.user.User;
import com.desofs.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    public CommentService(CommentRepository commentRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    // ── FR-16: Create comment ──────────────────────────────────────────────

    public CommentResponse addComment(Long taskId, String userEmail, CreateCommentRequest request) {
        validateContent(request.getContent());

        User user = resolveUser(userEmail);

        // Constructor validation ensures invariants (Week 8 DDD principles)
        Comment comment = new Comment(taskId, request.getContent().trim(), user.getId());

        return CommentResponse.from(commentRepository.save(comment));
    }

    // ── FR-16: List comments ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsForTask(Long taskId) {
        return commentRepository.findByTaskId(taskId)
                .stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }

    // ── FR-17: Update comment ────────────────────────────────────────────────

    public CommentResponse editComment(Long commentId, String userEmail, UpdateCommentRequest request) {
        validateContent(request.getContent());

        User user = resolveUser(userEmail);
        Comment comment = commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        // RBAC check: only author, manager, or admin can edit (SR-2, SR-4)
        if (!comment.getAuthorId().equals(user.getId()) && !isManagerOrAdmin(user)) {
            throw new IllegalArgumentException("Forbidden: You can only edit your own comments");
        }

        comment.setContent(request.getContent().trim());
        comment.setUpdatedAt(LocalDateTime.now());

        return CommentResponse.from(commentRepository.save(comment));
    }

    // ── FR-18: Soft-delete comment ───────────────────────────────────────────

    public void deleteComment(Long commentId, String userEmail) {
        User user = resolveUser(userEmail);
        Comment comment = commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        // RBAC check: only author, manager, or admin can delete (SR-2, SR-4)
        if (!comment.getAuthorId().equals(user.getId()) && !isManagerOrAdmin(user)) {
            throw new IllegalArgumentException("Forbidden: You can only delete your own comments");
        }

        // Soft delete: set timestamp instead of removing row (SR-9)
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
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