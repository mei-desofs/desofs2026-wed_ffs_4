package com.desofs.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.desofs.project.Project;
import com.desofs.project.ProjectRepository;
import com.desofs.user.User;
import com.desofs.user.UserRepository;

@Component
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

        // Seed test projects for users (only if empty)
        if (projectRepository.count() == 0) {
            User admin = userRepository.findByEmail("admin@example.com").orElseThrow();
            User manager = userRepository.findByEmail("manager@example.com").orElseThrow();
            User user = userRepository.findByEmail("user@example.com").orElseThrow();

            // Admin projects
            Project adminProject1 = new Project("Admin Dashboard", "System administration panel", admin);
            Project adminProject2 = new Project("User Management", "Manage system users and roles", admin);
            projectRepository.save(adminProject1);
            projectRepository.save(adminProject2);

            // Manager projects
            Project managerProject1 = new Project("Team Tasks", "Team collaboration board", manager);
            Project managerProject2 = new Project("Project Roadmap", "Q2 2026 development plan", manager);
            projectRepository.save(managerProject1);
            projectRepository.save(managerProject2);

            // User projects
            Project userProject1 = new Project("Personal Todo List", "Daily tasks and reminders", user);
            Project userProject2 = new Project("Learning Resources", "Java and Spring Boot tutorials", user);
            Project userProject3 = new Project("Side Project Ideas", "Future project concepts", user);
            projectRepository.save(userProject1);
            projectRepository.save(userProject2);
            projectRepository.save(userProject3);
        }
    }
}
