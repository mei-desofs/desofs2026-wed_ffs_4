package com.desofs.task;

import com.desofs.project.ProjectRepository;
import com.desofs.task.dto.AssignTaskRequest;
import com.desofs.task.dto.ChangeTaskStatusRequest;
import com.desofs.task.dto.CreateTaskRequest;
import com.desofs.task.dto.TaskResponse;
import com.desofs.task.dto.UpdateTaskRequest;
import com.desofs.user.User;
import com.desofs.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TaskService}.
 * All collaborators are Mockito mocks — no Spring context required.
 * Covers every branch across FR-10 to FR-15 to achieve 100 % code coverage.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService – unit tests")
class TaskServiceTest {

    @Mock private TaskRepository    taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository    userRepository;

    @InjectMocks
    private TaskService taskService;

    // ── Shared fixtures ──────────────────────────────────────────────────────

    private static final Long   PROJECT_ID    = 1L;
    private static final Long   OTHER_PROJECT = 99L;
    private static final UUID   TASK_ID       = UUID.randomUUID();
    private static final String CALLER_EMAIL  = "dev@example.com";
    private static final Long   CALLER_ID     = 42L;

    private User caller;

    @BeforeEach
    void setUp() {
        caller = new User();
        caller.setId(CALLER_ID);
        caller.setEmail(CALLER_EMAIL);
    }

    /** Build a minimal, non-deleted Task belonging to {@code PROJECT_ID}. */
    private Task buildTask() {
        Task t = new Task();
        t.setProjectId(PROJECT_ID);
        t.setTitle("Initial title");
        t.setCreatedBy(CALLER_ID);
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-10 – createTask
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FR-10 createTask")
    class CreateTask {

        private CreateTaskRequest req;

        @BeforeEach
        void initReq() {
            req = new CreateTaskRequest();
            req.setTitle("Do something");
        }

        @Test
        @DisplayName("null title throws IllegalArgumentException")
        void nullTitle_throws() {
            req.setTitle(null);
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("blank title throws IllegalArgumentException")
        void blankTitle_throws() {
            req.setTitle("   ");
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("title exceeding 255 chars throws IllegalArgumentException")
        void titleTooLong_throws() {
            req.setTitle("x".repeat(256));
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("project not found throws IllegalArgumentException")
        void projectNotFound_throws() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("caller user not found throws IllegalStateException")
        void callerNotFound_throws() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(userRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.empty());
            assertThrows(IllegalStateException.class,
                    () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("success – task created without description")
        void success_withoutDescription() {
            req.setDescription(null);
            Task saved = buildTask();
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(userRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.of(caller));
            when(taskRepository.save(any(Task.class))).thenReturn(saved);

            TaskResponse resp = taskService.createTask(PROJECT_ID, req, CALLER_EMAIL);

            assertNotNull(resp);
            assertEquals(PROJECT_ID, resp.getProjectId());
            assertNull(resp.getDescription());
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("success – task created with description (trimmed)")
        void success_withDescription() {
            req.setDescription("  Some description  ");
            Task saved = buildTask();
            saved.setDescription("Some description");
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(userRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.of(caller));
            when(taskRepository.save(any(Task.class))).thenReturn(saved);

            TaskResponse resp = taskService.createTask(PROJECT_ID, req, CALLER_EMAIL);

            assertNotNull(resp);
            assertEquals("Some description", resp.getDescription());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-11 – listTasksByProject
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FR-11 listTasksByProject")
    class ListTasks {

        @Test
        @DisplayName("project not found throws IllegalArgumentException")
        void projectNotFound_throws() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.listTasksByProject(PROJECT_ID));
        }

        @Test
        @DisplayName("returns empty list when project has no tasks")
        void emptyList() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(taskRepository.findByProjectIdAndDeletedFalse(PROJECT_ID)).thenReturn(List.of());

            List<TaskResponse> result = taskService.listTasksByProject(PROJECT_ID);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns mapped TaskResponse list")
        void returnsMappedList() {
            Task t1 = buildTask();
            t1.setTitle("Task A");
            Task t2 = buildTask();
            t2.setTitle("Task B");

            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(taskRepository.findByProjectIdAndDeletedFalse(PROJECT_ID))
                    .thenReturn(List.of(t1, t2));

            List<TaskResponse> result = taskService.listTasksByProject(PROJECT_ID);

            assertEquals(2, result.size());
            assertEquals("Task A", result.get(0).getTitle());
            assertEquals("Task B", result.get(1).getTitle());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-12 – updateTask
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FR-12 updateTask")
    class UpdateTask {

        @Test
        @DisplayName("task not found throws IllegalArgumentException")
        void taskNotFound_throws() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.updateTask(PROJECT_ID, TASK_ID, new UpdateTaskRequest()));
        }

        @Test
        @DisplayName("task belongs to different project throws IllegalArgumentException")
        void wrongProject_throws() {
            Task task = buildTask(); // belongs to PROJECT_ID
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));

            assertThrows(IllegalArgumentException.class,
                    () -> taskService.updateTask(OTHER_PROJECT, TASK_ID, new UpdateTaskRequest()));
        }

        @Test
        @DisplayName("blank title in request throws IllegalArgumentException")
        void blankTitle_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));

            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("  ");
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.updateTask(PROJECT_ID, TASK_ID, req));
        }

        @Test
        @DisplayName("title exceeding 255 chars throws IllegalArgumentException")
        void titleTooLong_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));

            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("y".repeat(256));
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.updateTask(PROJECT_ID, TASK_ID, req));
        }

        @Test
        @DisplayName("updates title only when description is null")
        void updatesTitleOnly() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);

            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("New title");

            TaskResponse resp = taskService.updateTask(PROJECT_ID, TASK_ID, req);

            assertEquals("New title", resp.getTitle());
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("updates description only when title is null")
        void updatesDescriptionOnly() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);

            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setDescription("  New description  ");

            taskService.updateTask(PROJECT_ID, TASK_ID, req);

            assertEquals("New description", task.getDescription());
        }

        @Test
        @DisplayName("updates both title and description")
        void updatesBothFields() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);

            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("New title");
            req.setDescription("New description");

            TaskResponse resp = taskService.updateTask(PROJECT_ID, TASK_ID, req);

            assertEquals("New title", resp.getTitle());
            assertEquals("New description", task.getDescription());
        }

        @Test
        @DisplayName("no-op update (both fields null) still saves the task")
        void noOpUpdate_stillSaves() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);

            taskService.updateTask(PROJECT_ID, TASK_ID, new UpdateTaskRequest());

            verify(taskRepository).save(task);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-13 – changeTaskStatus
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FR-13 changeTaskStatus")
    class ChangeTaskStatus {

        @Test
        @DisplayName("null status throws IllegalArgumentException")
        void nullStatus_throws() {
            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(null);
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req));
        }

        @Test
        @DisplayName("task not found throws IllegalArgumentException")
        void taskNotFound_throws() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.empty());
            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req));
        }

        @Test
        @DisplayName("task belongs to different project throws IllegalArgumentException")
        void wrongProject_throws() {
            Task task = buildTask(); // belongs to PROJECT_ID
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.changeTaskStatus(OTHER_PROJECT, TASK_ID, req));
        }

        @Test
        @DisplayName("invalid transition (TODO → DONE) throws IllegalArgumentException")
        void invalidTransition_throws() {
            Task task = buildTask();
            task.setStatus(TaskStatus.TODO);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.DONE);
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req));
        }

        @Test
        @DisplayName("valid transition TODO → IN_PROGRESS succeeds")
        void todoToInProgress_succeeds() {
            Task task = buildTask();
            task.setStatus(TaskStatus.TODO);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);

            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);

            TaskResponse resp = taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req);

            assertEquals(TaskStatus.IN_PROGRESS, resp.getStatus());
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("valid transition IN_PROGRESS → DONE succeeds")
        void inProgressToDone_succeeds() {
            Task task = buildTask();
            task.setStatus(TaskStatus.IN_PROGRESS);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);

            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.DONE);

            TaskResponse resp = taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req);

            assertEquals(TaskStatus.DONE, resp.getStatus());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-14 – deleteTask (soft-delete)
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FR-14 deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("task not found throws IllegalArgumentException")
        void taskNotFound_throws() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.empty());
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.deleteTask(PROJECT_ID, TASK_ID));
        }

        @Test
        @DisplayName("task belongs to different project throws IllegalArgumentException")
        void wrongProject_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.deleteTask(OTHER_PROJECT, TASK_ID));
        }

        @Test
        @DisplayName("soft-delete sets deleted=true and saves")
        void softDelete_setsFlagAndSaves() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));

            taskService.deleteTask(PROJECT_ID, TASK_ID);

            assertTrue(task.isDeleted());
            verify(taskRepository).save(task);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-15 – assignTask
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FR-15 assignTask")
    class AssignTask {

        @Test
        @DisplayName("task not found throws IllegalArgumentException")
        void taskNotFound_throws() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.empty());
            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.assignTask(PROJECT_ID, TASK_ID, req));
        }

        @Test
        @DisplayName("task belongs to different project throws IllegalArgumentException")
        void wrongProject_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.assignTask(OTHER_PROJECT, TASK_ID, req));
        }

        @Test
        @DisplayName("assignee user not found throws IllegalArgumentException")
        void assigneeNotFound_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            when(userRepository.existsById(5L)).thenReturn(false);

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);
            assertThrows(IllegalArgumentException.class,
                    () -> taskService.assignTask(PROJECT_ID, TASK_ID, req));
        }

        @Test
        @DisplayName("assigns task to valid user")
        void assignsTaskToUser() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            when(userRepository.existsById(5L)).thenReturn(true);
            when(taskRepository.save(task)).thenReturn(task);

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);

            TaskResponse resp = taskService.assignTask(PROJECT_ID, TASK_ID, req);

            assertEquals(5L, resp.getAssigneeId());
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("unassigns task when assigneeId is null")
        void unassignsTask_whenAssigneeIdIsNull() {
            Task task = buildTask();
            task.setAssigneeId(5L);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(null);

            TaskResponse resp = taskService.assignTask(PROJECT_ID, TASK_ID, req);

            assertNull(resp.getAssigneeId());
            // userRepository.existsById should NOT be called for null assigneeId
            verify(userRepository, never()).existsById(any());
            verify(taskRepository).save(task);
        }
    }
}

