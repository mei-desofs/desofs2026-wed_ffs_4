package com.desofs.task.dto;

import com.desofs.task.TaskStatus;

/**
 * Request body for FR-13: Change task status.
 * The target status must follow the pipeline: TODO → IN_PROGRESS → DONE.
 */
public class ChangeTaskStatusRequest {

    private TaskStatus status;

    public ChangeTaskStatusRequest() {}

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
}

