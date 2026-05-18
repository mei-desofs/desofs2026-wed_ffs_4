package com.desofs.comment;

import com.desofs.comment.dto.CommentResponse;
import com.desofs.comment.dto.CreateCommentRequest;
import com.desofs.comment.dto.UpdateCommentRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // ── FR-16: Create comment ────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> addComment(
            @PathVariable Long taskId,
            @RequestBody CreateCommentRequest request) {
        try {
            CommentResponse response = commentService.addComment(taskId, currentUserEmail(), request);
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    // ── FR-16: List comments ─────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> getComments(@PathVariable Long taskId) {
        try {
            List<CommentResponse> comments = commentService.getCommentsForTask(taskId);
            return ResponseEntity.ok(comments);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    // ── FR-17: Update comment ────────────────────────────────────────────────

    @PutMapping("/{commentId}")
    public ResponseEntity<?> editComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @RequestBody UpdateCommentRequest request) {
        try {
            CommentResponse response = commentService.editComment(commentId, currentUserEmail(), request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(ex.getMessage()));
        }
    }

    // ── FR-18: Delete comment ────────────────────────────────────────────────

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long taskId,
            @PathVariable Long commentId) {
        try {
            commentService.deleteComment(commentId, currentUserEmail());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(ex.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        throw new IllegalStateException("No authenticated user in context");
    }

    private java.util.Map<String, String> errorBody(String message) {
        return java.util.Map.of("error", message);
    }
}