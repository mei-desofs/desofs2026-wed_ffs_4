package com.desofs.task;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    /** FR-11: Retrieve all non-deleted tasks belonging to a project. */
    List<Task> findByProjectIdAndDeletedFalse(Long projectId);

    /** FR-12 / FR-13: Fetch a specific non-deleted task. */
    Optional<Task> findByIdAndDeletedFalse(UUID id);
}
