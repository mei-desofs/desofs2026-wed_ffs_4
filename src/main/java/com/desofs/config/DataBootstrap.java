package com.desofs.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.desofs.project.model.Project;
import com.desofs.project.repository.ProjectRepository;
import com.desofs.user.model.User;
import com.desofs.user.repository.UserRepository;

@Component
@Profile("!test")
public class DataBootstrap implements CommandLineRunner {
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    public DataBootstrap(UserRepository userRepository, ProjectRepository projectRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Seed test users only if they don't exist
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@example.com");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }

        if (userRepository.findByEmail("manager@example.com").isEmpty()) {
            User manager = new User();
            manager.setEmail("manager@example.com");
            manager.setPassword(passwordEncoder.encode("password123"));
            manager.setRole("MANAGER");
            userRepository.save(manager);
        }

        if (userRepository.findByEmail("user@example.com").isEmpty()) {
            User user = new User();
            user.setEmail("user@example.com");
            user.setPassword(passwordEncoder.encode("password123"));
            user.setRole("USER");
            userRepository.save(user);
        }

        // Seed test projects (only if empty)
        if (projectRepository.count() == 0) {
            User admin = userRepository.findByEmail("admin@example.com").orElseThrow();
            User manager = userRepository.findByEmail("manager@example.com").orElseThrow();
            User user = userRepository.findByEmail("user@example.com").orElseThrow();

            // All seeded projects are owned by ADMIN to match RBAC (only ADMIN creates projects)
            Project adminProject1 = new Project("Admin Dashboard", "System administration panel", admin);
            Project adminProject2 = new Project("User Management", "Manage system users and roles", admin);
            Project adminProject3 = new Project("Team Tasks", "Team collaboration board", admin);
            Project adminProject4 = new Project("Project Roadmap", "Q2 2026 development plan", admin);

            adminProject3.addMember(manager);
            adminProject3.addMember(user);
            adminProject4.addMember(manager);
            adminProject4.addMember(user);

            projectRepository.save(adminProject1);
            projectRepository.save(adminProject2);
            projectRepository.save(adminProject3);
            projectRepository.save(adminProject4);

        }
    }
}
