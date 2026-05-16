package com.desofs.comment.dto;

/**
 * Request body for FR-16: Create comment on task.
 */
public class CreateCommentRequest {

    private String content;

    public CreateCommentRequest() {}

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
} 
