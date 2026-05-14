package com.desofs.task;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TaskStatus#canTransitionTo(TaskStatus)}.
 * Covers every cell of the 3×3 transition matrix to achieve 100 % branch coverage.
 */
@DisplayName("TaskStatus – transition guard")
class TaskStatusTest {

    // ── Valid forward transitions ────────────────────────────────────────────

    @Test
    @DisplayName("TODO → IN_PROGRESS is allowed")
    void todoToInProgress_isAllowed() {
        assertTrue(TaskStatus.TODO.canTransitionTo(TaskStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("IN_PROGRESS → DONE is allowed")
    void inProgressToDone_isAllowed() {
        assertTrue(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.DONE));
    }

    // ── Invalid / reversal transitions ───────────────────────────────────────

    @Test
    @DisplayName("TODO → DONE is not allowed (skipping a step)")
    void todoToDone_isNotAllowed() {
        assertFalse(TaskStatus.TODO.canTransitionTo(TaskStatus.DONE));
    }

    @Test
    @DisplayName("TODO → TODO is not allowed (self-transition)")
    void todoToTodo_isNotAllowed() {
        assertFalse(TaskStatus.TODO.canTransitionTo(TaskStatus.TODO));
    }

    @Test
    @DisplayName("IN_PROGRESS → TODO is not allowed (reversal)")
    void inProgressToTodo_isNotAllowed() {
        assertFalse(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.TODO));
    }

    @Test
    @DisplayName("IN_PROGRESS → IN_PROGRESS is not allowed (self-transition)")
    void inProgressToInProgress_isNotAllowed() {
        assertFalse(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("DONE → TODO is not allowed (re-opening)")
    void doneToTodo_isNotAllowed() {
        assertFalse(TaskStatus.DONE.canTransitionTo(TaskStatus.TODO));
    }

    @Test
    @DisplayName("DONE → IN_PROGRESS is not allowed (re-opening)")
    void doneToInProgress_isNotAllowed() {
        assertFalse(TaskStatus.DONE.canTransitionTo(TaskStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("DONE → DONE is not allowed (self-transition)")
    void doneToDone_isNotAllowed() {
        assertFalse(TaskStatus.DONE.canTransitionTo(TaskStatus.DONE));
    }
}

