package com.desofs.task.dto;

/**
 * Request body for FR-15: Assign task to a member.
 * Pass null assigneeId to unassign the task.
 */
public class AssignTaskRequest {

    private Long assigneeId;

    public AssignTaskRequest() {}

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
}

