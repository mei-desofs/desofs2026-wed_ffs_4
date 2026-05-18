package com.desofs.task.dto;

public class CreateTaskRequest {
    private String title;
    private String description;
    private Long assignedTo;
    private String priority;

    public CreateTaskRequest() {}
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
