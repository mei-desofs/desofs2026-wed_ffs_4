package com.desofs.project.service;

import org.springframework.stereotype.Service;

import com.desofs.project.model.Project;
import com.desofs.project.repository.ProjectRepository;
import com.desofs.user.User;
import com.desofs.user.UserRepository;

@Service
public class ProjectMemberService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectMemberService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    public Project addMember(Long projectId, User actor, String memberEmail) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!canManageMembers(actor, project)) {
            throw new RuntimeException("Forbidden");
        }

        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (project.getMembers() == null) {
            throw new RuntimeException("Project members not initialized");
        }

        project.addMember(member);
        return projectRepository.save(project);
    }

    public Project removeMember(Long projectId, User actor, String memberEmail) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!canManageMembers(actor, project)) {
            throw new RuntimeException("Forbidden");
        }

        User member = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (project.getMembers() == null) {
            throw new RuntimeException("Project members not initialized");
        }

        project.getMembers().removeIf(m -> m.getId() != null && m.getId().equals(member.getId()));
        return projectRepository.save(project);
    }

    private boolean canManageMembers(User actor, Project project) {
        if ("ADMIN".equals(actor.getRole())) {
            return true;
        }
        if ("MANAGER".equals(actor.getRole())) {
            return project.getMembers() != null && project.getMembers().stream()
                    .anyMatch(member -> member.getId() != null && member.getId().equals(actor.getId()));
        }
        return false;
    }
}