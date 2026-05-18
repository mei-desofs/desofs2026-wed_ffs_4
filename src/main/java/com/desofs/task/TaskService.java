package com.desofs.task;

import com.desofs.project.repository.ProjectRepository;
import com.desofs.task.dto.AssignTaskRequest;
import com.desofs.task.dto.ChangeTaskStatusRequest;
import com.desofs.task.dto.CreateTaskRequest;
import com.desofs.task.dto.TaskDetailResponse;
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
 * FR-10 – createTask: create a task inside an existing project.
 * FR-11 – listTasks: list all non-deleted tasks that belong to a project.
 * FR-12 – updateTask: edit the title and/or description of an existing task.
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
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public TaskResponse createTask(Long projectId, CreateTaskRequest request, String callerEmail) {
        validateTitle(request.getTitle());

        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        User caller = resolveUser(callerEmail);
        verifyCallerProjectAccess(projectId, caller);

        Task task = new Task();
        task.setProjectId(projectId);
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        task.setStatus(TaskStatus.TODO);
        task.setPriority(parsePriority(request.getPriority()));
        task.setCreatedBy(caller.getId());

        if (request.getAssignedTo() != null) {
            validateAssignee(projectId, request.getAssignedTo());
            task.setAssigneeId(request.getAssignedTo());
        }

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasksByProject(Long projectId) {
        return listTasksByProject(projectId, (String) null, null);
    }

    public List<TaskResponse> listTasksByProject(Long projectId, String callerEmail) {
        return listTasksByProject(projectId, callerEmail, null);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasksByProject(Long projectId, String callerEmail, TaskStatus status) {
        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }

        User caller = resolveUser(callerEmail);
        verifyCallerProjectAccess(projectId, caller);

        return (status == null
                ? taskRepository.findByProjectIdAndDeletedFalse(projectId)
                : taskRepository.findByProjectIdAndDeletedFalseAndStatus(projectId, status))
                .stream()
                .map(TaskResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDetailResponse getTask(Long projectId, UUID taskId) {
        return TaskDetailResponse.from(getTaskOrThrow(projectId, taskId));
    }

    public TaskResponse updateTask(Long projectId, UUID taskId, UpdateTaskRequest request) {
        return updateTask(projectId, taskId, request, null);
    }

    public TaskResponse updateTask(Long projectId, UUID taskId, UpdateTaskRequest request, String callerEmail) {
        Task task = getTaskOrThrow(projectId, taskId);

        User caller = resolveUser(callerEmail);
        verifyCallerProjectAccess(projectId, caller);

        if (request.getTitle() != null) {
            validateTitle(request.getTitle());
            task.setTitle(request.getTitle().trim());
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription().trim());
        }

        if (request.getPriority() != null) {
            task.setPriority(parsePriority(request.getPriority()));
        }

        if (request.getAssignedTo() != null) {
            validateAssignee(projectId, request.getAssignedTo());
            task.setAssigneeId(request.getAssignedTo());
        }

        task.setUpdatedAt(LocalDateTime.now());
        return TaskResponse.from(taskRepository.save(task));
    }

    public TaskResponse changeTaskStatus(Long projectId, UUID taskId, ChangeTaskStatusRequest request, String callerEmail) {
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Status must not be null");
        }

        Task task = getTaskOrThrow(projectId, taskId);
        User caller = resolveUser(callerEmail);
        verifyCallerProjectAccess(projectId, caller);

        boolean isManagerOrAbove = isManagerOrAdmin(caller.getRole());
        boolean isAssignee = Objects.equals(task.getAssigneeId(), caller.getId());
        if (!isManagerOrAbove && !isAssignee) {
            throw new AccessDeniedException("Only the assigned member or a manager/admin may change this task's status");
        }

        if (!task.getStatus().canTransitionTo(request.getStatus())) {
            throw new IllegalArgumentException("Invalid status transition: " + task.getStatus() + " → " + request.getStatus());
        }

        task.setStatus(request.getStatus());
        task.setUpdatedAt(LocalDateTime.now());
        return TaskResponse.from(taskRepository.save(task));
    }

    public void deleteTask(Long projectId, UUID taskId) {
        deleteTask(projectId, taskId, null);
    }

    public void deleteTask(Long projectId, UUID taskId, String callerEmail) {
        Task task = getTaskOrThrow(projectId, taskId);
        User caller = resolveUser(callerEmail);
        verifyCallerProjectAccess(projectId, caller);
        task.setDeleted(true);
        task.setUpdatedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    public TaskResponse assignTask(Long projectId, UUID taskId, AssignTaskRequest request, String callerEmail) {
        Task task = getTaskOrThrow(projectId, taskId);

        User caller = resolveUser(callerEmail);
        verifyCallerProjectAccess(projectId, caller);

        if (!isManagerOrAdmin(caller.getRole())) {
            if (request.getAssigneeId() == null || !request.getAssigneeId().equals(caller.getId())) {
                throw new AccessDeniedException("Members may only self-assign tasks");
            }
            if (task.getAssigneeId() != null) {
                throw new AccessDeniedException("Members may only self-assign unassigned tasks");
            }
        }

        if (request.getAssigneeId() != null) {
            validateAssignee(projectId, request.getAssigneeId());
        }

        task.setAssigneeId(request.getAssigneeId());
        task.setUpdatedAt(LocalDateTime.now());
        return TaskResponse.from(taskRepository.save(task));
    }

    private User resolveUser(String email) {
        if (email == null) {
            throw new IllegalStateException("Authenticated user not found: null");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    private void verifyCallerProjectAccess(Long projectId, User caller) {
        if ("ADMIN".equals(caller.getRole())) {
            return;
        }
        if (!projectRepository.isUserProjectMember(projectId, caller.getId())) {
            throw new AccessDeniedException("Caller is not a member of project: " + projectId);
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }
        if (title.trim().length() > 255) {
            throw new IllegalArgumentException("Task title must not exceed 255 characters");
        }
    }

    private Task getTaskOrThrow(Long projectId, UUID taskId) {
        Task task = taskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        if (!task.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("Task does not belong to project: " + projectId);
        }
        return task;
    }

    private TaskPriority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return TaskPriority.MEDIUM;
        }
        return TaskPriority.valueOf(priority.trim().toUpperCase());
    }

    private void validateAssignee(Long projectId, Long assigneeId) {
        if (!userRepository.existsById(assigneeId)) {
            throw new IllegalArgumentException("User not found: " + assigneeId);
        }
        if (!projectRepository.isUserProjectMember(projectId, assigneeId)) {
            throw new IllegalArgumentException("User " + assigneeId + " is not a member of project " + projectId);
        }
    }

    private boolean isManagerOrAdmin(String role) {
        return "ADMIN".equals(role) || "MANAGER".equals(role);
    }
}
