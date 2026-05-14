package com.desofs.task.dto;

/**
 * Request body for FR-10: Create task in project.
 */
public class CreateTaskRequest {

    private String title;
    private String description;

    public CreateTaskRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

