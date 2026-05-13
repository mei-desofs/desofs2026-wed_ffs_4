package com.desofs.project;

import java.util.List;
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

import com.desofs.user.User;
import com.desofs.user.UserRepository;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectController(ProjectService projectService, ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
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
    public ResponseEntity<?> createProject(@RequestBody Map<String, String> body) {
        try {
            String email = getCurrentUserEmail();
            User owner = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            Project project = projectService.createProject(body.get("name"), body.get("description"), owner);
            return ResponseEntity.status(201).body(Map.of(
                    "id", project.getId(),
                    "name", project.getName(),
                    "description", project.getDescription()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listProjects() {
        try {
            String email = getCurrentUserEmail();
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            List<Project> projects = projectService.getUserProjects(user.getId());
            return ResponseEntity.ok(projects);
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable Long id) {
        try {
            String email = getCurrentUserEmail();
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            Project project = projectService.getProjectById(id, user.getId());
            return ResponseEntity.ok(Map.of(
                    "id", project.getId(),
                    "name", project.getName(),
                    "description", project.getDescription()
            ));
        } catch (RuntimeException ex) {
            if ("Project not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
            }
            if ("Forbidden".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
            }
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id) {
        try {
            String email = getCurrentUserEmail();
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            projectService.deleteProject(id, user.getId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            if ("Project not found".equals(ex.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
            }
            if ("Forbidden".equals(ex.getMessage())) {
                return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
            }
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        }
    }
}
