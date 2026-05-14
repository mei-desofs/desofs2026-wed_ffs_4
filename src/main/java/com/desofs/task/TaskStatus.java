package com.desofs.task;

/**
 * Lifecycle states of a Task.
 * Transitions are strictly one-directional: TODO → IN_PROGRESS → DONE.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE;

    /**
     * Returns true only if moving from this state to {@code next} is a valid
     * forward transition. Reversals and re-opening DONE tasks are not allowed (FR-13).
     */
    public boolean canTransitionTo(TaskStatus next) {
        return switch (this) {
            case TODO        -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == DONE;
            case DONE        -> false;
        };
    }
}
