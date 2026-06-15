package com.desofs.task.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.desofs.task.model.Task;
import com.desofs.task.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    /** FR-11: Retrieve all non-deleted tasks belonging to a project. */
    List<Task> findByProjectIdAndDeletedFalse(Long projectId);

    /** FR-11: Retrieve non-deleted tasks belonging to a project filtered by status. */
    List<Task> findByProjectIdAndDeletedFalseAndStatus(Long projectId, TaskStatus status);

    /** FR-12 / FR-13: Fetch a specific non-deleted task. */
    Optional<Task> findByIdAndDeletedFalse(UUID id);
}
