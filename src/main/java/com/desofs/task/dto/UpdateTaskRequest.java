package com.desofs.task.dto;

/**
 * Request body for FR-12: Update task details (title and/or description).
 * Both fields are optional — only non-null values are applied.
 */
public class UpdateTaskRequest {

    private String title;
    private String description;

    public UpdateTaskRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

