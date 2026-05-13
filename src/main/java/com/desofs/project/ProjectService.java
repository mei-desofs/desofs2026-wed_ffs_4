package com.desofs.project;

import java.util.List;

import org.springframework.stereotype.Service;

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

    public void deleteProject(Long projectId, Long ownerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (project.getOwner() == null || project.getOwner().getId() == null || !project.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }

        projectRepository.delete(project);
    }

    public List<Project> getUserProjects(Long userId) {
        return projectRepository.findByOwnerId(userId);
    }

    public Project getProjectById(Long projectId, Long ownerId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (project.getOwner() == null || project.getOwner().getId() == null || !project.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Forbidden");
        }

        return project;
    }
}
