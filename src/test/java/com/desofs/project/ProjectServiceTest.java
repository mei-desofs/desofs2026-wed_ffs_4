package com.desofs.project;

import com.desofs.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProjectShouldPersistProjectForOwner() {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("user@example.com");

        Project saved = new Project("Project 1", "Description", owner);
        saved.setId(10L);

        when(projectRepository.save(any(Project.class))).thenReturn(saved);

        Project result = projectService.createProject("Project 1", "Description", owner);

        assertEquals(10L, result.getId());
        assertEquals("Project 1", result.getName());
        assertEquals("Description", result.getDescription());
        assertEquals(owner, result.getOwner());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void getUserProjectsShouldReturnMemberProjectsForManager() {
        User manager = new User();
        manager.setId(5L);
        manager.setRole("MANAGER");

        Project p1 = new Project();
        p1.setId(1L);
        p1.setName("Project A");

        Project p2 = new Project();
        p2.setId(2L);
        p2.setName("Project B");

        when(projectRepository.findByMembersId(5L)).thenReturn(List.of(p1, p2));

        List<Project> result = projectService.getUserProjects(manager);

        assertEquals(2, result.size());
        assertEquals("Project A", result.get(0).getName());
        assertEquals("Project B", result.get(1).getName());
        verify(projectRepository).findByMembersId(5L);
    }

    @Test
    void getUserProjectsShouldReturnMemberProjectsForUser() {
        User user = new User();
        user.setId(8L);
        user.setRole("USER");

        Project p1 = new Project();
        p1.setId(11L);
        p1.setName("Team Tasks");

        when(projectRepository.findByMembersId(8L)).thenReturn(List.of(p1));

        List<Project> result = projectService.getUserProjects(user);

        assertEquals(1, result.size());
        assertEquals("Team Tasks", result.get(0).getName());
        verify(projectRepository).findByMembersId(8L);
    }

    @Test
    void deleteProjectShouldRemoveOwnedProject() {
        User owner = new User();
        owner.setId(5L);

        Project project = new Project("Project X", "Description X", owner);
        project.setId(99L);

        when(projectRepository.findById(99L)).thenReturn(java.util.Optional.of(project));

        projectService.deleteProject(99L, 5L);

        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProjectShouldRejectNonOwner() {
        User owner = new User();
        owner.setId(5L);

        Project project = new Project("Project X", "Description X", owner);
        project.setId(99L);

        when(projectRepository.findById(99L)).thenReturn(java.util.Optional.of(project));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> projectService.deleteProject(99L, 7L));
        assertTrue(ex.getMessage().contains("Forbidden"));

        verify(projectRepository, never()).delete(any());
    }

    @Test
    void getProjectByIdShouldReturnOwnedProject() {
        User manager = new User();
        manager.setId(5L);
        manager.setRole("MANAGER");

        Project project = new Project("Project Y", "Description Y", manager);
        project.setId(88L);
        project.addMember(manager);

        when(projectRepository.findById(88L)).thenReturn(java.util.Optional.of(project));

        Project result = projectService.getProjectById(88L, manager);

        assertEquals(88L, result.getId());
        assertEquals("Project Y", result.getName());
        verify(projectRepository).findById(88L);
    }

    @Test
    void getProjectByIdShouldAllowUserWhenMember() {
        User user = new User();
        user.setId(9L);
        user.setRole("USER");

        Project project = new Project("Project Y", "Description Y", user);
        project.setId(88L);
        project.addMember(user);

        when(projectRepository.findById(88L)).thenReturn(java.util.Optional.of(project));

        Project result = projectService.getProjectById(88L, user);

        assertEquals(88L, result.getId());
    }

    @Test
    void getProjectByIdShouldRejectUserWhenNotMember() {
        User user = new User();
        user.setId(9L);
        user.setRole("USER");

        User adminOwner = new User();
        adminOwner.setId(3L);
        adminOwner.setRole("ADMIN");

        Project project = new Project("Project Y", "Description Y", adminOwner);
        project.setId(88L);

        when(projectRepository.findById(88L)).thenReturn(java.util.Optional.of(project));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> projectService.getProjectById(88L, user));
        assertTrue(ex.getMessage().contains("Forbidden"));
    }

    @Test
    void getProjectByIdShouldThrowWhenNotFound() {
        when(projectRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> projectService.getProjectById(999L, admin));
        assertTrue(ex.getMessage().contains("Project not found"));
    }
}
