package com.desofs.project.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desofs.project.dto.ProjectMemberRequest;
import com.desofs.project.model.Project;
import com.desofs.project.service.ProjectMemberService;
import com.desofs.user.User;
import com.desofs.user.UserRepository;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;
    private final UserRepository userRepository;

    public ProjectMemberController(ProjectMemberService projectMemberService, UserRepository userRepository) {
        this.projectMemberService = projectMemberService;
        this.userRepository = userRepository;
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        throw new RuntimeException("Unauthorized");
    }

    @PostMapping
    public ResponseEntity<?> addMember(@PathVariable Long projectId, @RequestBody ProjectMemberRequest body) {
        try {
            String email = getCurrentUserEmail();
            User actor = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            Project project = projectMemberService.addMember(projectId, actor, body.getEmail());
            return ResponseEntity.ok(Map.of(
                    "projectId", project.getId(),
                    "membersCount", project.getMembers().size()
            ));
        } catch (RuntimeException ex) {
            if ("Project not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
            }
            if ("User not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
            }
            if ("Forbidden".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
            }
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> removeMember(@PathVariable Long projectId, @RequestBody ProjectMemberRequest body) {
        try {
            String email = getCurrentUserEmail();
            User actor = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            Project project = projectMemberService.removeMember(projectId, actor, body.getEmail());
            return ResponseEntity.ok(Map.of(
                    "projectId", project.getId(),
                    "membersCount", project.getMembers().size()
            ));
        } catch (RuntimeException ex) {
            if ("Project not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
            }
            if ("User not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
            }
            if ("Forbidden".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
            }
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        }
    }
}