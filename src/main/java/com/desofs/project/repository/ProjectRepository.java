package com.desofs.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.desofs.project.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByOwnerId(Long ownerId);
    List<Project> findByMembersId(Long userId);
    List<Project> findByMembersIdAndDeletedFalse(Long userId);
    List<Project> findByMembersIdAndDeletedTrue(Long userId);
    List<Project> findAllByDeletedFalse();
    List<Project> findAllByDeletedTrue();
    Optional<Project> findByIdAndDeletedFalse(Long projectId);
}