package com.desofs.task;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.desofs.task.dto.AssignTaskRequest;
import com.desofs.task.dto.ChangeTaskStatusRequest;
import com.desofs.task.dto.CreateTaskRequest;
import com.desofs.task.dto.TaskDetailResponse;
import com.desofs.task.dto.TaskResponse;
import com.desofs.task.dto.UpdateTaskRequest;

import com.desofs.task.TaskStatus;

/**
 * REST controller for Task endpoints, nested under /api/projects/{projectId}/tasks.
 *
 * POST   /api/projects/{projectId}/tasks            – FR-10: create task
 * GET    /api/projects/{projectId}/tasks            – FR-11: list tasks per project
 * PUT    /api/projects/{projectId}/tasks/{taskId}   – FR-12: update task
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // ── FR-10: Create task ───────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<?> createTask(@PathVariable Long projectId, @RequestBody CreateTaskRequest request) {
        try {
            return ResponseEntity.status(201).body(taskService.createTask(projectId, request, currentUserEmail()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    // ── FR-11: List tasks per project ────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> listTasks(@PathVariable Long projectId) {
        try {
            List<TaskResponse> tasks = taskService.listTasksByProject(projectId, currentUserEmail());
            return ResponseEntity.ok(tasks);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    @GetMapping(params = "status")
    public ResponseEntity<?> listTasksByStatus(@PathVariable Long projectId, @RequestParam String status) {
        try {
            List<TaskResponse> tasks = taskService.listTasksByProject(
                    projectId,
                    currentUserEmail(),
                    TaskStatus.valueOf(status.trim().toUpperCase()));
            return ResponseEntity.ok(tasks);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTask(@PathVariable Long projectId, @PathVariable UUID taskId) {
        try {
            TaskDetailResponse response = taskService.getTask(projectId, taskId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable Long projectId, @PathVariable UUID taskId, @RequestBody UpdateTaskRequest request) {
        try {
            return ResponseEntity.ok(taskService.updateTask(projectId, taskId, request, currentUserEmail()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<?> changeTaskStatus(@PathVariable Long projectId, @PathVariable UUID taskId, @RequestBody ChangeTaskStatusRequest request) {
        try {
            return ResponseEntity.ok(taskService.changeTaskStatus(projectId, taskId, request, currentUserEmail()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long projectId, @PathVariable UUID taskId) {
        try {
            taskService.deleteTask(projectId, taskId, currentUserEmail());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    @PatchMapping("/{taskId}/assignee")
    public ResponseEntity<?> assignTask(@PathVariable Long projectId, @PathVariable UUID taskId, @RequestBody AssignTaskRequest request) {
        try {
            return ResponseEntity.ok(taskService.assignTask(projectId, taskId, request, currentUserEmail()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        throw new IllegalStateException("No authenticated user in context");
    }

    private java.util.Map<String, String> errorBody(String message) {
        return java.util.Map.of("error", message);
    }
}
