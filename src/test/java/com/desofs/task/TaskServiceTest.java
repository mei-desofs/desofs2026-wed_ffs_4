package com.desofs.task;

import com.desofs.project.repository.ProjectRepository;
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
import org.springframework.security.access.AccessDeniedException;

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
@SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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

    /** Stub caller as ADMIN (bypasses project membership check). */
    private void stubAdminCaller() {
        caller.setRole("ADMIN");
        when(userRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.of(caller));
    }

    /** Stub caller as MANAGER who IS a project member. */
    private void stubManagerCallerAsMember() {
        caller.setRole("MANAGER");
        when(userRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.of(caller));
        when(projectRepository.isUserProjectMember(PROJECT_ID, CALLER_ID)).thenReturn(true);
    }

    /** Stub caller as USER who IS a project member. */
    private void stubUserCallerAsMember() {
        caller.setRole("USER");
        when(userRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.of(caller));
        when(projectRepository.isUserProjectMember(PROJECT_ID, CALLER_ID)).thenReturn(true);
    }

    /** Stub caller as MANAGER who is NOT a project member. */
    private void stubManagerCallerNotMember() {
        caller.setRole("MANAGER");
        when(userRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.of(caller));
        when(projectRepository.isUserProjectMember(PROJECT_ID, CALLER_ID)).thenReturn(false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-10 – createTask
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FR-10 createTask")
    @SuppressWarnings("unused")
    class CreateTask {

        private CreateTaskRequest req;

        @BeforeEach
        @SuppressWarnings("unused")
        void initReq() {
            req = new CreateTaskRequest();
            req.setTitle("Do something");
        }

        @Test
        @DisplayName("null title throws IllegalArgumentException")
        void nullTitle_throws() {
            req.setTitle(null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("blank title throws IllegalArgumentException")
        void blankTitle_throws() {
            req.setTitle("   ");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("title exceeding 255 chars throws IllegalArgumentException")
        void titleTooLong_throws() {
            req.setTitle("x".repeat(256));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("project not found throws IllegalArgumentException")
        void projectNotFound_throws() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("caller user not found throws IllegalStateException")
        void callerNotFound_throws() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(userRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.empty());
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("caller not a project member throws AccessDeniedException")
        void callerNotProjectMember_throws() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            stubManagerCallerNotMember();
            assertThrows(AccessDeniedException.class,
                () -> taskService.createTask(PROJECT_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("success – task created without description")
        void success_withoutDescription() {
            req.setDescription(null);
            Task saved = buildTask();
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            stubAdminCaller();
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
            stubAdminCaller();
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
    @SuppressWarnings("unused")
    class ListTasks {

        @Test
        @DisplayName("project not found throws IllegalArgumentException")
        void projectNotFound_throws() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.listTasksByProject(PROJECT_ID, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("caller not a project member throws AccessDeniedException")
        void callerNotProjectMember_throws() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            stubManagerCallerNotMember();
            assertThrows(AccessDeniedException.class,
                () -> taskService.listTasksByProject(PROJECT_ID, CALLER_EMAIL));
        }

        @Test
        @DisplayName("returns empty list when project has no tasks")
        void emptyList() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            stubAdminCaller();
            when(taskRepository.findByProjectIdAndDeletedFalse(PROJECT_ID)).thenReturn(List.of());

            List<TaskResponse> result = taskService.listTasksByProject(PROJECT_ID, CALLER_EMAIL);

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
            stubAdminCaller();
            when(taskRepository.findByProjectIdAndDeletedFalse(PROJECT_ID))
                    .thenReturn(List.of(t1, t2));

            List<TaskResponse> result = taskService.listTasksByProject(PROJECT_ID, CALLER_EMAIL);

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
    @SuppressWarnings("unused")
    class UpdateTask {

        @Test
        @DisplayName("task not found throws IllegalArgumentException")
        void taskNotFound_throws() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.empty());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.updateTask(PROJECT_ID, TASK_ID, new UpdateTaskRequest(), CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("task belongs to different project throws IllegalArgumentException")
        void wrongProject_throws() {
            Task task = buildTask(); // belongs to PROJECT_ID
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.updateTask(OTHER_PROJECT, TASK_ID, new UpdateTaskRequest(), CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("caller not a project member throws AccessDeniedException")
        void callerNotProjectMember_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubManagerCallerNotMember();

            assertThrows(AccessDeniedException.class,
                () -> taskService.updateTask(PROJECT_ID, TASK_ID, new UpdateTaskRequest(), CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("blank title in request throws IllegalArgumentException")
        void blankTitle_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubAdminCaller();

            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("  ");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.updateTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("title exceeding 255 chars throws IllegalArgumentException")
        void titleTooLong_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubAdminCaller();

            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("y".repeat(256));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.updateTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("updates title only when description is null")
        void updatesTitleOnly() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubAdminCaller();
            when(taskRepository.save(task)).thenReturn(task);

            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("New title");

            TaskResponse resp = taskService.updateTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL);

            assertEquals("New title", resp.getTitle());
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("updates description only when title is null")
        void updatesDescriptionOnly() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubAdminCaller();
            when(taskRepository.save(task)).thenReturn(task);

            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setDescription("  New description  ");

            taskService.updateTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL);

            assertEquals("New description", task.getDescription());
        }

        @Test
        @DisplayName("updates both title and description")
        void updatesBothFields() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubAdminCaller();
            when(taskRepository.save(task)).thenReturn(task);

            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("New title");
            req.setDescription("New description");

            TaskResponse resp = taskService.updateTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL);

            assertEquals("New title", resp.getTitle());
            assertEquals("New description", task.getDescription());
        }

        @Test
        @DisplayName("no-op update (both fields null) still saves the task")
        void noOpUpdate_stillSaves() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubAdminCaller();
            when(taskRepository.save(task)).thenReturn(task);

            taskService.updateTask(PROJECT_ID, TASK_ID, new UpdateTaskRequest(), CALLER_EMAIL);

            verify(taskRepository).save(task);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-13 – changeTaskStatus
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FR-13 changeTaskStatus")
    @SuppressWarnings("unused")
    class ChangeTaskStatus {

        @Test
        @DisplayName("null status throws IllegalArgumentException")
        void nullStatus_throws() {
            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("task not found throws IllegalArgumentException")
        void taskNotFound_throws() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.empty());
            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("task belongs to different project throws IllegalArgumentException")
        void wrongProject_throws() {
            Task task = buildTask(); // belongs to PROJECT_ID
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.changeTaskStatus(OTHER_PROJECT, TASK_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("caller not a project member throws AccessDeniedException")
        void callerNotProjectMember_throws() {
            Task task = buildTask();
            task.setStatus(TaskStatus.TODO);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubManagerCallerNotMember();

            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);

            assertThrows(AccessDeniedException.class,
                () -> taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("invalid transition (TODO → DONE) throws IllegalArgumentException")
        void invalidTransition_throws() {
            Task task = buildTask();
            task.setStatus(TaskStatus.TODO);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubManagerCallerAsMember();

            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.DONE);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("valid transition TODO → IN_PROGRESS succeeds for MANAGER")
        void todoToInProgress_manager_succeeds() {
            Task task = buildTask();
            task.setStatus(TaskStatus.TODO);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubManagerCallerAsMember();
            when(taskRepository.save(task)).thenReturn(task);

            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);

            TaskResponse resp = taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, CALLER_EMAIL);

            assertEquals(TaskStatus.IN_PROGRESS, resp.getStatus());
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("valid transition IN_PROGRESS → DONE succeeds for ADMIN (bypasses membership)")
        void inProgressToDone_admin_succeeds() {
            Task task = buildTask();
            task.setStatus(TaskStatus.IN_PROGRESS);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubAdminCaller();
            when(taskRepository.save(task)).thenReturn(task);

            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.DONE);

            TaskResponse resp = taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, CALLER_EMAIL);

            assertEquals(TaskStatus.DONE, resp.getStatus());
        }

        @Test
        @DisplayName("FR-13/SR-4: assigned MEMBER may change status")
        void assignedMember_mayChangeStatus() {
            Task task = buildTask();
            task.setStatus(TaskStatus.TODO);
            task.setAssigneeId(CALLER_ID); // caller is the assignee
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubUserCallerAsMember();
            when(taskRepository.save(task)).thenReturn(task);

            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);

            TaskResponse resp = taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, CALLER_EMAIL);

            assertEquals(TaskStatus.IN_PROGRESS, resp.getStatus());
        }

        @Test
        @DisplayName("FR-13/SR-4: non-assigned MEMBER is denied")
        void nonAssignedMember_isDenied() {
            Task task = buildTask();
            task.setStatus(TaskStatus.TODO);
            task.setAssigneeId(999L); // someone else
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubUserCallerAsMember();

            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);

            assertThrows(AccessDeniedException.class,
                () -> taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("FR-13/SR-4: unassigned MEMBER is denied")
        void unassignedTask_memberIsDenied() {
            Task task = buildTask();
            task.setStatus(TaskStatus.TODO);
            task.setAssigneeId(null); // no assignee
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubUserCallerAsMember();

            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);

            assertThrows(AccessDeniedException.class,
                () -> taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-14 – deleteTask (soft-delete)
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FR-14 deleteTask")
    @SuppressWarnings("unused")
    class DeleteTask {

        @Test
        @DisplayName("task not found throws IllegalArgumentException")
        void taskNotFound_throws() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.empty());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.deleteTask(PROJECT_ID, TASK_ID, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("task belongs to different project throws IllegalArgumentException")
        void wrongProject_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.deleteTask(OTHER_PROJECT, TASK_ID, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("caller not a project member throws AccessDeniedException")
        void callerNotProjectMember_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubManagerCallerNotMember();

            assertThrows(AccessDeniedException.class,
                () -> taskService.deleteTask(PROJECT_ID, TASK_ID, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("soft-delete sets deleted=true and saves")
        void softDelete_setsFlagAndSaves() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubAdminCaller();

            taskService.deleteTask(PROJECT_ID, TASK_ID, CALLER_EMAIL);

            assertTrue(task.isDeleted());
            verify(taskRepository).save(task);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-15 – assignTask
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("FR-15 assignTask")
    @SuppressWarnings("unused")
    class AssignTask {

        @Test
        @DisplayName("task not found throws IllegalArgumentException")
        void taskNotFound_throws() {
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.empty());
            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.assignTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("task belongs to different project throws IllegalArgumentException")
        void wrongProject_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.assignTask(OTHER_PROJECT, TASK_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("caller not a project member throws AccessDeniedException")
        void callerNotProjectMember_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubManagerCallerNotMember();

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);

            assertThrows(AccessDeniedException.class,
                () -> taskService.assignTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("assignee user not found throws IllegalArgumentException")
        void assigneeNotFound_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubManagerCallerAsMember();
            when(userRepository.existsById(5L)).thenReturn(false);

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.assignTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("MANAGER assigns task to valid project member")
        void manager_assignsTaskToUser() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubManagerCallerAsMember();
            when(userRepository.existsById(5L)).thenReturn(true);
            when(projectRepository.isUserProjectMember(PROJECT_ID, 5L)).thenReturn(true);
            when(taskRepository.save(task)).thenReturn(task);

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);

            TaskResponse resp = taskService.assignTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL);

            assertEquals(5L, resp.getAssigneeId());
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("FR-15/SR-4: MANAGER cannot assign to user outside the project")
        void manager_assignsToNonMember_throws() {
            Task task = buildTask();
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubManagerCallerAsMember();
            when(userRepository.existsById(5L)).thenReturn(true);
            when(projectRepository.isUserProjectMember(PROJECT_ID, 5L)).thenReturn(false);

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> taskService.assignTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            assertNotNull(ex.getMessage());
            assertTrue(ex.getMessage().contains("not a member"));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("MANAGER unassigns task when assigneeId is null")
        void manager_unassignsTask_whenAssigneeIdIsNull() {
            Task task = buildTask();
            task.setAssigneeId(5L);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubManagerCallerAsMember();
            when(taskRepository.save(task)).thenReturn(task);

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(null);

            TaskResponse resp = taskService.assignTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL);

            assertNull(resp.getAssigneeId());
            verify(userRepository, never()).existsById(any());
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("FR-15/SR-4: MEMBER self-assigns to unassigned task in project")
        void member_selfAssigns_unassignedTask() {
            Task task = buildTask();
            task.setAssigneeId(null); // unassigned
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubUserCallerAsMember();
            when(userRepository.existsById(CALLER_ID)).thenReturn(true);
            // CALLER_ID is both the caller and the assignee, so the same stub covers both checks
            when(taskRepository.save(task)).thenReturn(task);

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(CALLER_ID); // self-assign

            TaskResponse resp = taskService.assignTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL);

            assertEquals(CALLER_ID, resp.getAssigneeId());
        }

        @Test
        @DisplayName("FR-15/SR-4: MEMBER self-assign rejected when caller is not a project member")
        void member_selfAssigns_notProjectMember_throws() {
            Task task = buildTask();
            task.setAssigneeId(null);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            // caller is NOT a member → verifyCallerProjectAccess fires before assignee checks
            caller.setRole("USER");
            when(userRepository.findByEmail(CALLER_EMAIL)).thenReturn(Optional.of(caller));
            when(projectRepository.isUserProjectMember(PROJECT_ID, CALLER_ID)).thenReturn(false);

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(CALLER_ID);

            assertThrows(AccessDeniedException.class,
                () -> taskService.assignTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("FR-15/SR-4: MEMBER denied when assigning to someone else")
        void member_denied_assigningToOther() {
            Task task = buildTask();
            task.setAssigneeId(null);
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubUserCallerAsMember();

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(999L); // not the caller

            assertThrows(AccessDeniedException.class,
                () -> taskService.assignTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("FR-15/SR-4: MEMBER denied when task is already assigned")
        void member_denied_taskAlreadyAssigned() {
            Task task = buildTask();
            task.setAssigneeId(5L); // already assigned to someone else
            when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
            stubUserCallerAsMember();

            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(CALLER_ID);

            assertThrows(AccessDeniedException.class,
                () -> taskService.assignTask(PROJECT_ID, TASK_ID, req, CALLER_EMAIL));
            verify(taskRepository, never()).save(any());
        }
    }
}

