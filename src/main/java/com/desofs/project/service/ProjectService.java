package com.desofs.project.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.desofs.project.model.Project;
import com.desofs.project.repository.ProjectRepository;
import com.desofs.user.User;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project createProject(String name, String description, User owner) {
        Project project = new Project(name, description, owner);
        return projectRepository.save(project);
    }

    public void deleteProject(Long projectId, User user) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Forbidden");
        }

        project.setDeleted(true);
        projectRepository.save(project);
    }

    public List<Project> getUserProjects(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return projectRepository.findAllByDeletedFalse();
        }
        if ("MANAGER".equals(user.getRole()) || "USER".equals(user.getRole())) {
            return projectRepository.findByMembersIdAndDeletedFalse(user.getId());
        }
        throw new RuntimeException("Forbidden");
    }

    public Project getProjectById(Long projectId, User user) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if ("ADMIN".equals(user.getRole())) {
            return project;
        }

        if ("MANAGER".equals(user.getRole()) || "USER".equals(user.getRole())) {
            if (project.getMembers() != null && project.getMembers().stream()
                    .anyMatch(member -> member.getId() != null && member.getId().equals(user.getId()))) {
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
            return projectRepository.save(project);
        }

        if ("MANAGER".equals(user.getRole())) {
            if (project.getMembers() != null && project.getMembers().stream()
                    .anyMatch(member -> member.getId() != null && member.getId().equals(user.getId()))) {
                project.setName(name);
                project.setDescription(description);
                return projectRepository.save(project);
            }
            throw new RuntimeException("Forbidden");
        }

        throw new RuntimeException("Forbidden");
    }
}