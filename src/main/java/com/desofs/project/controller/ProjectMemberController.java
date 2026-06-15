package com.desofs.project.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desofs.audit.model.AuditAction;
import com.desofs.audit.service.AuditService;
import com.desofs.project.dto.ProjectMemberRequest;
import com.desofs.project.model.Project;
import com.desofs.project.service.ProjectMemberService;
import com.desofs.user.model.User;
import com.desofs.user.repository.UserRepository;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ProjectMemberController(ProjectMemberService projectMemberService, UserRepository userRepository, AuditService auditService) {
        this.projectMemberService = projectMemberService;
        this.userRepository = userRepository;
        this.auditService = auditService;
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
            Project project = body.getUserId() != null
                ? projectMemberService.addMember(projectId, actor, body.getUserId())
                : projectMemberService.addMember(projectId, actor, body.getEmail());
            auditService.record(email, AuditAction.PROJECT_MEMBER_ADD, "project", String.valueOf(projectId), true, "Added member " + (body.getUserId() != null ? body.getUserId() : body.getEmail()));
            return ResponseEntity.ok(Map.of(
                    "projectId", project.getId(),
                    "membersCount", project.getMembers().size()
            ));
        } catch (RuntimeException ex) {
            auditService.record(getCurrentUserEmail(), AuditAction.PROJECT_MEMBER_ADD, "project", String.valueOf(projectId), false, ex.getMessage());
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

    @GetMapping
    public ResponseEntity<?> listMembers(@PathVariable Long projectId) {
        try {
            String email = getCurrentUserEmail();
            User actor = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            Project project = projectMemberService.getMembers(projectId, actor);
            auditService.record(email, AuditAction.PROJECT_READ, "project", String.valueOf(projectId), true, "Listed members");
            return ResponseEntity.ok(project.getMembers().stream().map(member -> Map.of(
                    "user_id", member.getId(),
                    "role_in_project", member.getRole(),
                    "joined_at", null
            )).toList());
        } catch (RuntimeException ex) {
            auditService.record(getCurrentUserEmail(), AuditAction.PROJECT_READ, "project", String.valueOf(projectId), false, ex.getMessage());
            if ("Project not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
            }
            if ("Forbidden".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
            }
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<?> removeMemberById(@PathVariable Long projectId, @PathVariable Long memberId) {
        try {
            String email = getCurrentUserEmail();
            User actor = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            Project project = projectMemberService.removeMember(projectId, actor, memberId);
            auditService.record(email, AuditAction.PROJECT_MEMBER_REMOVE, "project", String.valueOf(projectId), true, "Removed member id " + memberId);
            return ResponseEntity.ok(Map.of(
                    "projectId", project.getId(),
                    "membersCount", project.getMembers().size()
            ));
        } catch (RuntimeException ex) {
            auditService.record(getCurrentUserEmail(), AuditAction.PROJECT_MEMBER_REMOVE, "project", String.valueOf(projectId), false, ex.getMessage());
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