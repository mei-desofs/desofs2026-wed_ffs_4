package com.desofs.comment.dto;

/**
 * Request body for FR-17: Update comment.
 */
public class UpdateCommentRequest {

    private String content;

    public UpdateCommentRequest() {}

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}