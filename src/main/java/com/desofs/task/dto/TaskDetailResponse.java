package com.desofs.task.dto;

import com.desofs.task.Task;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TaskDetailResponse {
    private UUID id;
    private Long projectId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private Long assigneeId;
    private Long createdBy;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private List<Map<String, Object>> comments = List.of();
    private List<Map<String, Object>> attachments = List.of();

    public static TaskDetailResponse from(Task task) {
        TaskDetailResponse r = new TaskDetailResponse();
        r.id = task.getId();
        r.projectId = task.getProjectId();
        r.title = task.getTitle();
        r.description = task.getDescription();
        r.status = task.getStatus() != null ? task.getStatus().name() : null;
        r.priority = task.getPriority() != null ? task.getPriority().name() : null;
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
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public Long getAssigneeId() { return assigneeId; }
    public Long getCreatedBy() { return createdBy; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<Map<String, Object>> getComments() { return comments; }
    public void setComments(List<Map<String, Object>> comments) { this.comments = comments; }
    public List<Map<String, Object>> getAttachments() { return attachments; }
    public void setAttachments(List<Map<String, Object>> attachments) { this.attachments = attachments; }
}
