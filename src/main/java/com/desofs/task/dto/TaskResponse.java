package com.desofs.task.dto;

import com.desofs.task.Task;
import com.desofs.task.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read model returned by all Task endpoints.
 * Keeps the API contract stable and decoupled from the JPA entity.
 */
public class TaskResponse {

    private UUID id;
    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private Long assigneeId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TaskResponse() {}

    /** Convenience factory — build response directly from the entity. */
    public static TaskResponse from(Task task) {
        TaskResponse r = new TaskResponse();
        r.id          = task.getId();
        r.projectId   = task.getProjectId();
        r.title       = task.getTitle();
        r.description = task.getDescription();
        r.status      = task.getStatus();
        r.assigneeId  = task.getAssigneeId();
        r.createdBy   = task.getCreatedBy();
        r.createdAt   = task.getCreatedAt();
        r.updatedAt   = task.getUpdatedAt();
        return r;
    }

    public Long getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TaskStatus getStatus() { return status; }
    public Long getAssigneeId() { return assigneeId; }
}