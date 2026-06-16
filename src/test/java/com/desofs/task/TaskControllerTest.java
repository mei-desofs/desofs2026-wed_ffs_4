package com.desofs.task;

import com.desofs.task.controller.TaskController;
import com.desofs.task.dto.AssignTaskRequest;
import com.desofs.task.dto.ChangeTaskStatusRequest;
import com.desofs.task.dto.CreateTaskRequest;
import com.desofs.task.dto.TaskResponse;
import com.desofs.task.dto.UpdateTaskRequest;
import com.desofs.task.model.Task;
import com.desofs.task.model.TaskStatus;
import com.desofs.task.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link TaskController}.
 * The controller is instantiated directly (no Spring context / MockMvc) so that the
 * {@code SecurityContextHolder} can be controlled explicitly — this is the only way to
 * cover the defensive {@code currentUserEmail()} throw branch (auth == null) without
 * bypassing Spring Security's filter chain.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskController – unit tests")
@SuppressWarnings("unused")
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController controller;

    private static final Long   PROJECT_ID = 1L;
    private static final UUID   TASK_ID    = UUID.randomUUID();
    private static final String USER_EMAIL = "dev@example.com";

    /** Builds a minimal {@link TaskResponse} for stubbing. */
    private TaskResponse dummyResponse() {
        Task task = new Task();
        task.setProjectId(PROJECT_ID);
        task.setTitle("A task");
        task.setCreatedBy(1L);
        task.setUpdatedAt(LocalDateTime.now());
        return TaskResponse.from(task);
    }

    /** Installs a fully-authenticated principal in the SecurityContextHolder. */
    private void setAuthenticatedUser(String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ════════════════════════════════════════════════════════════════════════
    // currentUserEmail helper – all three branches
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("currentUserEmail()")
    class CurrentUserEmail {

        @Test
        @DisplayName("throws IllegalStateException when SecurityContext has no Authentication")
        void noAuthentication_throwsIllegalState() {
            // SecurityContextHolder was cleared in @AfterEach; do it here too for clarity
            SecurityContextHolder.clearContext();

            CreateTaskRequest req = new CreateTaskRequest();
            req.setTitle("title");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> controller.createTask(PROJECT_ID, req));
            assertEquals("No authenticated user in context", ex.getMessage());
        }

        @Test
        @DisplayName("throws IllegalStateException when Authentication.isAuthenticated() is false")
        void notAuthenticated_throwsIllegalState() {
            Authentication notAuth = mock(Authentication.class);
            when(notAuth.isAuthenticated()).thenReturn(false);
            SecurityContextHolder.getContext().setAuthentication(notAuth);

            CreateTaskRequest req = new CreateTaskRequest();
            req.setTitle("title");

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> controller.createTask(PROJECT_ID, req));
            assertEquals("No authenticated user in context", ex.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-10 – POST /api/projects/{projectId}/tasks
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST createTask")
    class CreateTask {

        @BeforeEach
        void auth() { setAuthenticatedUser(USER_EMAIL); }

        @Test
        @DisplayName("201 Created when service succeeds")
        void success_returns201() {
            CreateTaskRequest req = new CreateTaskRequest();
            req.setTitle("New task");
            when(taskService.createTask(PROJECT_ID, req, USER_EMAIL)).thenReturn(dummyResponse());

            ResponseEntity<?> resp = controller.createTask(PROJECT_ID, req);

            assertEquals(HttpStatus.CREATED, resp.getStatusCode());
            assertNotNull(resp.getBody());
        }

        @Test
        @DisplayName("400 Bad Request when service throws IllegalArgumentException")
        void serviceThrows_returns400() {
            CreateTaskRequest req = new CreateTaskRequest();
            req.setTitle("");
            when(taskService.createTask(eq(PROJECT_ID), eq(req), anyString()))
                    .thenThrow(new IllegalArgumentException("Task title must not be blank"));

            ResponseEntity<?> resp = controller.createTask(PROJECT_ID, req);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-11 – GET /api/projects/{projectId}/tasks
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET listTasks")
    class ListTasks {

        @BeforeEach
        void auth() { setAuthenticatedUser(USER_EMAIL); }

        @Test
        @DisplayName("200 OK with task list")
        void success_returns200() {
            when(taskService.listTasksByProject(PROJECT_ID, USER_EMAIL)).thenReturn(List.of(dummyResponse()));

            ResponseEntity<?> resp = controller.listTasks(PROJECT_ID);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
        }

        @Test
        @DisplayName("400 Bad Request when project not found")
        void projectNotFound_returns400() {
            when(taskService.listTasksByProject(PROJECT_ID, USER_EMAIL))
                    .thenThrow(new IllegalArgumentException("Project not found"));

            ResponseEntity<?> resp = controller.listTasks(PROJECT_ID);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        }

        @Test
        @DisplayName("AccessDeniedException propagates when caller is not a project member")
        void callerNotMember_propagatesAccessDenied() {
            when(taskService.listTasksByProject(PROJECT_ID, USER_EMAIL))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("not a member"));

            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.listTasks(PROJECT_ID));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-12 – PUT /api/projects/{projectId}/tasks/{taskId}
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PUT updateTask")
    class UpdateTask {

        @BeforeEach
        void auth() { setAuthenticatedUser(USER_EMAIL); }

        @Test
        @DisplayName("200 OK with updated task")
        void success_returns200() {
            UpdateTaskRequest req = new UpdateTaskRequest();
            req.setTitle("Updated");
            when(taskService.updateTask(PROJECT_ID, TASK_ID, req, USER_EMAIL)).thenReturn(dummyResponse());

            ResponseEntity<?> resp = controller.updateTask(PROJECT_ID, TASK_ID, req);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
        }

        @Test
        @DisplayName("400 Bad Request when task not found")
        void taskNotFound_returns400() {
            UpdateTaskRequest req = new UpdateTaskRequest();
            when(taskService.updateTask(PROJECT_ID, TASK_ID, req, USER_EMAIL))
                    .thenThrow(new IllegalArgumentException("Task not found"));

            ResponseEntity<?> resp = controller.updateTask(PROJECT_ID, TASK_ID, req);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        }

        @Test
        @DisplayName("AccessDeniedException propagates when caller is not a project member")
        void callerNotMember_propagatesAccessDenied() {
            UpdateTaskRequest req = new UpdateTaskRequest();
            when(taskService.updateTask(PROJECT_ID, TASK_ID, req, USER_EMAIL))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("not a member"));

            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.updateTask(PROJECT_ID, TASK_ID, req));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-13 – PATCH /api/projects/{projectId}/tasks/{taskId}/status
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PATCH changeTaskStatus")
    class ChangeTaskStatus {

        @BeforeEach
        void auth() { setAuthenticatedUser(USER_EMAIL); }

        @Test
        @DisplayName("200 OK with updated status")
        void success_returns200() {
            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.IN_PROGRESS);
            when(taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, USER_EMAIL)).thenReturn(dummyResponse());

            ResponseEntity<?> resp = controller.changeTaskStatus(PROJECT_ID, TASK_ID, req);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
        }

        @Test
        @DisplayName("400 Bad Request on invalid transition")
        void invalidTransition_returns400() {
            ChangeTaskStatusRequest req = new ChangeTaskStatusRequest();
            req.setStatus(TaskStatus.DONE);
            when(taskService.changeTaskStatus(PROJECT_ID, TASK_ID, req, USER_EMAIL))
                    .thenThrow(new IllegalArgumentException("Invalid status transition"));

            ResponseEntity<?> resp = controller.changeTaskStatus(PROJECT_ID, TASK_ID, req);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-14 – DELETE /api/projects/{projectId}/tasks/{taskId}
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE deleteTask")
    class DeleteTask {

        @BeforeEach
        void auth() { setAuthenticatedUser(USER_EMAIL); }

        @Test
        @DisplayName("204 No Content on successful soft-delete")
        void success_returns204() {
            doNothing().when(taskService).deleteTask(PROJECT_ID, TASK_ID, USER_EMAIL);

            ResponseEntity<?> resp = controller.deleteTask(PROJECT_ID, TASK_ID);

            assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        }

        @Test
        @DisplayName("400 Bad Request when task not found")
        void taskNotFound_returns400() {
            doThrow(new IllegalArgumentException("Task not found"))
                    .when(taskService).deleteTask(PROJECT_ID, TASK_ID, USER_EMAIL);

            ResponseEntity<?> resp = controller.deleteTask(PROJECT_ID, TASK_ID);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        }

        @Test
        @DisplayName("AccessDeniedException propagates when caller is not a project member")
        void callerNotMember_propagatesAccessDenied() {
            doThrow(new org.springframework.security.access.AccessDeniedException("not a member"))
                    .when(taskService).deleteTask(PROJECT_ID, TASK_ID, USER_EMAIL);

            assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.deleteTask(PROJECT_ID, TASK_ID));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FR-15 – PATCH /api/projects/{projectId}/tasks/{taskId}/assignee
    // ════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PATCH assignTask")
    class AssignTask {

        @BeforeEach
        void auth() { setAuthenticatedUser(USER_EMAIL); }

        @Test
        @DisplayName("200 OK when task is assigned")
        void success_returns200() {
            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(5L);
            when(taskService.assignTask(PROJECT_ID, TASK_ID, req, USER_EMAIL)).thenReturn(dummyResponse());

            ResponseEntity<?> resp = controller.assignTask(PROJECT_ID, TASK_ID, req);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
        }

        @Test
        @DisplayName("400 Bad Request when assignee not found")
        void assigneeNotFound_returns400() {
            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(99L);
            when(taskService.assignTask(PROJECT_ID, TASK_ID, req, USER_EMAIL))
                    .thenThrow(new IllegalArgumentException("User not found: 99"));

            ResponseEntity<?> resp = controller.assignTask(PROJECT_ID, TASK_ID, req);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        }

        @Test
        @DisplayName("400 Bad Request when assignee is not a member of the project")
        void assigneeNotProjectMember_returns400() {
            AssignTaskRequest req = new AssignTaskRequest();
            req.setAssigneeId(99L);
            when(taskService.assignTask(PROJECT_ID, TASK_ID, req, USER_EMAIL))
                    .thenThrow(new IllegalArgumentException("User 99 is not a member of project 1"));

            ResponseEntity<?> resp = controller.assignTask(PROJECT_ID, TASK_ID, req);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        }
    }
}
