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

    public List<Project> getUserProjects(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return projectRepository.findAll();
        }
        if ("MANAGER".equals(user.getRole()) || "USER".equals(user.getRole())) {
            return projectRepository.findByMembersId(user.getId());
        }
        throw new RuntimeException("Forbidden");
    }

    public Project getProjectById(Long projectId, User user) {
        Project project = projectRepository.findById(projectId)
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
}
