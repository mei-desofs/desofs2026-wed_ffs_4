package com.desofs.task;

import com.desofs.task.dto.AssignTaskRequest;
import com.desofs.task.dto.ChangeTaskStatusRequest;
import com.desofs.task.dto.CreateTaskRequest;
import com.desofs.task.dto.TaskResponse;
import com.desofs.task.dto.UpdateTaskRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<?> createTask(
            @PathVariable Long projectId,
            @RequestBody CreateTaskRequest request) {
        try {
            TaskResponse response = taskService.createTask(projectId, request, currentUserEmail());
            return ResponseEntity.status(201).body(response);
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

    // ── FR-12: Update task ───────────────────────────────────────────────────

    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(
            @PathVariable Long projectId,
            @PathVariable UUID taskId,
            @RequestBody UpdateTaskRequest request) {
        try {
            TaskResponse response = taskService.updateTask(projectId, taskId, request, currentUserEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    // ── FR-13: Change task status ────────────────────────────────────────────

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<?> changeTaskStatus(
            @PathVariable Long projectId,
            @PathVariable UUID taskId,
            @RequestBody ChangeTaskStatusRequest request) {
        try {
            TaskResponse response = taskService.changeTaskStatus(projectId, taskId, request, currentUserEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    // ── FR-14: Soft-delete task ──────────────────────────────────────────────

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(
            @PathVariable Long projectId,
            @PathVariable UUID taskId) {
        try {
            taskService.deleteTask(projectId, taskId, currentUserEmail());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    // ── FR-15: Assign task to member ─────────────────────────────────────────

    @PatchMapping("/{taskId}/assignee")
    public ResponseEntity<?> assignTask(
            @PathVariable Long projectId,
            @PathVariable UUID taskId,
            @RequestBody AssignTaskRequest request) {
        try {
            TaskResponse response = taskService.assignTask(projectId, taskId, request, currentUserEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
