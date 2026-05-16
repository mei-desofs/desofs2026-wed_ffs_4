package com.desofs.task;

import com.desofs.project.repository.ProjectRepository;
import com.desofs.task.dto.AssignTaskRequest;
import com.desofs.task.dto.ChangeTaskStatusRequest;
import com.desofs.task.dto.CreateTaskRequest;
import com.desofs.task.dto.TaskResponse;
import com.desofs.task.dto.UpdateTaskRequest;
import com.desofs.user.User;
import com.desofs.user.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Business logic for Task management.
 *
 * FR-10 – createTask  : create a task inside an existing project.
 * FR-11 – listTasks   : list all non-deleted tasks that belong to a project.
 * FR-12 – updateTask  : edit the title and/or description of an existing task.
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository) {
        this.taskRepository  = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository  = userRepository;
    }

    // ── FR-10 ────────────────────────────────────────────────────────────────

    /**
     * Creates a new task in the given project.
     *
     * @param projectId   the project the task belongs to
     * @param request     title (required) and optional description
     * @param callerEmail email of the authenticated user, used to record createdBy
     * @return the persisted task as a response DTO
     * @throws IllegalArgumentException if title is blank or the project does not exist
     */
    public TaskResponse createTask(Long projectId, CreateTaskRequest request, String callerEmail) {
        validateTitle(request.getTitle());

        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        User caller = resolveUser(callerEmail);

        Task task = new Task();
        task.setProjectId(projectId);
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        task.setStatus(TaskStatus.TODO);
        task.setCreatedBy(caller.getId());

        return TaskResponse.from(taskRepository.save(task));
    }

    // ── FR-11 ────────────────────────────────────────────────────────────────

    /**
     * Lists all non-deleted tasks for a project.
     *
     * @param projectId the project whose tasks are requested
     * @return list of task response DTOs (may be empty)
     * @throws IllegalArgumentException if the project does not exist
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> listTasksByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        return taskRepository.findByProjectIdAndDeletedFalse(projectId)
                .stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());
    }

    // ── FR-12 ────────────────────────────────────────────────────────────────

    /**
     * Updates the title and/or description of an existing task.
     * Only fields that are non-null in the request are applied (partial update).
     *
     * @param projectId the project the task belongs to (used for ownership validation)
     * @param taskId    UUID of the task to update
     * @param request   the fields to change
     * @return updated task as a response DTO
     * @throws IllegalArgumentException if the task does not exist or does not belong to the project
     */
    public TaskResponse updateTask(Long projectId, UUID taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (!task.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Task does not belong to project: " + projectId);
        }

        if (request.getTitle() != null) {
            validateTitle(request.getTitle());
            task.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription().trim());
        }

        task.setUpdatedAt(LocalDateTime.now());

        return TaskResponse.from(taskRepository.save(task));
    }

    // ── FR-13: Change task status ─────────────────────────────────────────────

    /**
     * Advances the task status following the pipeline: TODO → IN_PROGRESS → DONE.
     * Reversals are not permitted.
     *
     * FR-13 / SR-4: ADMIN and MANAGER may change the status of any task in the project.
     * A MEMBER may only change the status of a task they are personally assigned to.
     *
     * @throws IllegalArgumentException  if status is null, task not found, wrong project,
     *                                   or the transition is invalid
     * @throws AccessDeniedException     if caller is a MEMBER who is not the task assignee
     */
    public TaskResponse changeTaskStatus(Long projectId, UUID taskId,
                                         ChangeTaskStatusRequest request, String callerEmail) {
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Status must not be null");
        }

        Task task = taskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (!task.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Task does not belong to project: " + projectId);
        }

        // FR-13 / SR-4: only ADMIN/MANAGER or the assigned MEMBER may update status
        User caller = resolveUser(callerEmail);
        boolean isManagerOrAbove = isManagerOrAdmin(caller.getRole());
        boolean isAssignee = Objects.equals(task.getAssigneeId(), caller.getId());
        if (!isManagerOrAbove && !isAssignee) {
            throw new AccessDeniedException(
                    "Only the assigned member or a manager/admin may change this task's status");
        }

        if (!task.getStatus().canTransitionTo(request.getStatus())) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + task.getStatus() + " → " + request.getStatus());
        }

        task.setStatus(request.getStatus());
        task.setUpdatedAt(LocalDateTime.now());

        return TaskResponse.from(taskRepository.save(task));
    }

    // ── FR-14: Soft-delete task ───────────────────────────────────────────────

    /**
     * Soft-deletes a task — the record is preserved in the DB but excluded from all
     * query results (SR-9).
     *
     * @throws IllegalArgumentException if task not found or does not belong to the project
     */
    public void deleteTask(Long projectId, UUID taskId) {
        Task task = taskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (!task.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Task does not belong to project: " + projectId);
        }

        task.setDeleted(true);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    // ── FR-15: Assign task to member ──────────────────────────────────────────

    /**
     * Assigns (or unassigns) a task to a user.
     * Pass {@code null} as assigneeId to remove the current assignee.
     *
     * FR-15: ADMIN and MANAGER may assign tasks to any project member or unassign them.
     * A MEMBER may only self-assign to a task that currently has no assignee.
     *
     * @throws IllegalArgumentException if task not found, wrong project, or target user not found
     * @throws AccessDeniedException    if caller is a MEMBER trying to assign someone else,
     *                                  or trying to reassign an already-assigned task
     */
    public TaskResponse assignTask(Long projectId, UUID taskId,
                                   AssignTaskRequest request, String callerEmail) {
        Task task = taskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (!task.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Task does not belong to project: " + projectId);
        }

        // FR-15 / SR-4: MANAGER/ADMIN can assign anyone; MEMBER may only self-assign to unassigned tasks
        User caller = resolveUser(callerEmail);
        if (!isManagerOrAdmin(caller.getRole())) {
            if (task.getAssigneeId() != null) {
                throw new AccessDeniedException(
                        "Task is already assigned; only a manager/admin may reassign it");
            }
            if (!Objects.equals(request.getAssigneeId(), caller.getId())) {
                throw new AccessDeniedException(
                        "Members may only assign themselves to unassigned tasks");
            }
        }

        if (request.getAssigneeId() != null && !userRepository.existsById(request.getAssigneeId())) {
            throw new IllegalArgumentException("User not found: " + request.getAssigneeId());
        }

        task.setAssigneeId(request.getAssigneeId());
        task.setUpdatedAt(LocalDateTime.now());

        return TaskResponse.from(taskRepository.save(task));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    private boolean isManagerOrAdmin(String role) {
        return "ADMIN".equals(role) || "MANAGER".equals(role);
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }
        if (title.trim().length() > 255) {
            throw new IllegalArgumentException("Task title must not exceed 255 characters");
        }
    }
}
