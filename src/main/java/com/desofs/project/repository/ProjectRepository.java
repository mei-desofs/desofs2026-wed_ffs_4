package com.desofs.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.desofs.project.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerId(Long ownerId);
    List<Project> findByMembersId(Long userId);
    List<Project> findByMembersIdAndDeletedFalse(Long userId);
    List<Project> findByMembersIdAndDeletedTrue(Long userId);
    List<Project> findAllByDeletedFalse();
    List<Project> findAllByDeletedTrue();
    Optional<Project> findByIdAndDeletedFalse(Long projectId);

    /**
     * Returns true when the given user is the project owner OR is in the project_members table.
     * Used to enforce FR-15: only project participants may be assigned to tasks.
     */
    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE p.id = :projectId " +
           "AND (p.owner.id = :userId OR EXISTS " +
           "  (SELECT m FROM p.members m WHERE m.id = :userId))")
    boolean isUserProjectMember(@Param("projectId") Long projectId, @Param("userId") Long userId);
}