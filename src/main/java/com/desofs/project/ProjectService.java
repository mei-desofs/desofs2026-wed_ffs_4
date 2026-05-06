package com.desofs.project;

import com.desofs.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    private Long getCurrentUserId() {
        // For Phase 1: extract email from JWT and resolve to user ID
        // For now, return a placeholder - will be refined later
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            // TODO: get user from DB using email from token
            return null;
        }
        throw new RuntimeException("Unauthorized");
    }

    public Project createProject(String name, String description, User owner) {
        Project project = new Project(name, description, owner);
        return projectRepository.save(project);
    }

    public List<Project> getUserProjects(Long userId) {
        return projectRepository.findByOwnerId(userId);
    }
}
