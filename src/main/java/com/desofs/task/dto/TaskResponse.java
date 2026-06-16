package com.desofs.task.dto;

import com.desofs.task.model.Task;
import com.desofs.task.model.TaskPriority;
import com.desofs.task.model.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class TaskResponse {
    private UUID id;
    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assigneeId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TaskResponse() {}
    public static TaskResponse from(Task task) {
        TaskResponse r = new TaskResponse();
        r.id = task.getId();
        r.projectId = task.getProjectId();
        r.title = task.getTitle();
        r.description = task.getDescription();
        r.status = task.getStatus();
        r.priority = task.getPriority();
        r.assigneeId = task.getAssigneeId();
        r.createdBy = task.getCreatedBy();
        r.createdAt = task.getCreatedAt();
        r.updatedAt = task.getUpdatedAt();
        return r;
    }
    public UUID getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public TaskPriority getPriority() { return priority; }
    public Long getAssigneeId() { return assigneeId; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
