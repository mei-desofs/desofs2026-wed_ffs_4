package com.desofs.comment.dto;

import com.desofs.comment.Comment;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;

/**
 * Read model returned by all Comment endpoints.
 * Decouples API contract from JPA entity.
 * Applies XSS protection on output (SR-7).
 */
public class CommentResponse {

    private Long id;
    private Long taskId;
    private String content;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CommentResponse() {}

    /** Factory method: builds response from entity with XSS protection. */
    public static CommentResponse from(Comment comment) {
        CommentResponse r = new CommentResponse();
        r.id = comment.getId();
        r.taskId = comment.getTaskId();
        r.content = HtmlUtils.htmlEscape(comment.getContent());
        r.authorId = comment.getAuthorId();
        r.createdAt = comment.getCreatedAt();
        r.updatedAt = comment.getUpdatedAt();
        return r;
    }

    public Long getId() { return id; }
    public Long getTaskId() { return taskId; }
    public String getContent() { return content; }
    public Long getAuthorId() { return authorId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
} 
