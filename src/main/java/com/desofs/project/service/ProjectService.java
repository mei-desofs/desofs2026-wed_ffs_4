package com.desofs.project.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.desofs.audit.model.AuditAction;
import com.desofs.audit.service.AuditService;
import com.desofs.project.model.Project;
import com.desofs.project.repository.ProjectRepository;
import com.desofs.user.model.User;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final AuditService auditService;

    @Autowired
    public ProjectService(ProjectRepository projectRepository, AuditService auditService) {
        this.projectRepository = projectRepository;
        this.auditService = auditService;
    }

    public Project createProject(String name, String description, User owner) {
        Project project = new Project(name, description, owner);
        Project saved = projectRepository.save(project);
        recordAudit(owner.getEmail(), AuditAction.PROJECT_CREATE, "project", String.valueOf(saved.getId()), true, "Project created");
        return saved;
    }

    public void deleteProject(Long projectId, User user) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Forbidden");
        }

        project.setDeleted(true);
        projectRepository.save(project);
        recordAudit(user.getEmail(), AuditAction.PROJECT_DELETE, "project", String.valueOf(projectId), true, "Project deleted");
    }

    public List<Project> getUserProjects(User user, String status) {
        boolean archived = "archived".equalsIgnoreCase(status);
        if ("ADMIN".equals(user.getRole())) {
            recordAudit(user.getEmail(), AuditAction.PROJECT_READ, "project", "collection", true, "Listed projects");
            return archived ? projectRepository.findAllByDeletedTrue() : projectRepository.findAllByDeletedFalse();
        }
        if ("MANAGER".equals(user.getRole()) || "USER".equals(user.getRole())) {
            recordAudit(user.getEmail(), AuditAction.PROJECT_READ, "project", "collection", true, "Listed projects");
            return archived ? projectRepository.findByMembersIdAndDeletedTrue(user.getId()) : projectRepository.findByMembersIdAndDeletedFalse(user.getId());
        }
        throw new RuntimeException("Forbidden");
    }

    public List<Project> getUserProjects(User user) {
        return getUserProjects(user, null);
    }

    public Project getProjectById(Long projectId, User user) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if ("ADMIN".equals(user.getRole())) {
            recordAudit(user.getEmail(), AuditAction.PROJECT_READ, "project", String.valueOf(projectId), true, "Project retrieved");
            return project;
        }

        if ("MANAGER".equals(user.getRole()) || "USER".equals(user.getRole())) {
            if (project.getMembers() != null && project.getMembers().stream()
                    .anyMatch(member -> member.getId() != null && member.getId().equals(user.getId()))) {
                recordAudit(user.getEmail(), AuditAction.PROJECT_READ, "project", String.valueOf(projectId), true, "Project retrieved");
                return project;
            }
            throw new RuntimeException("Forbidden");
        }

        throw new RuntimeException("Forbidden");

    }

    public Project updateProject(Long projectId, User user, String name, String description) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if ("ADMIN".equals(user.getRole())) {
            project.setName(name);
            project.setDescription(description);
            Project saved = projectRepository.save(project);
            recordAudit(user.getEmail(), AuditAction.PROJECT_UPDATE, "project", String.valueOf(projectId), true, "Project updated");
            return saved;
        }

        if ("MANAGER".equals(user.getRole())) {
            if (project.getMembers() != null && project.getMembers().stream()
                    .anyMatch(member -> member.getId() != null && member.getId().equals(user.getId()))) {
                project.setName(name);
                project.setDescription(description);
                Project saved = projectRepository.save(project);
                recordAudit(user.getEmail(), AuditAction.PROJECT_UPDATE, "project", String.valueOf(projectId), true, "Project updated");
                return saved;
            }
            throw new RuntimeException("Forbidden");
        }

        throw new RuntimeException("Forbidden");
    }

    private void recordAudit(String actor, AuditAction action, String resourceType, String resourceId, boolean success, String details) {
        if (auditService != null) {
            auditService.record(actor, action, resourceType, resourceId, success, details);
        }
    }
}