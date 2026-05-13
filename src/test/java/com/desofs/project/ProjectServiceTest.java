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
    void getUserProjectsShouldReturnProjectsForUserId() {
        Project p1 = new Project();
        p1.setId(1L);
        p1.setName("Project A");

        Project p2 = new Project();
        p2.setId(2L);
        p2.setName("Project B");

        when(projectRepository.findByOwnerId(5L)).thenReturn(List.of(p1, p2));

        List<Project> result = projectService.getUserProjects(5L);

        assertEquals(2, result.size());
        assertEquals("Project A", result.get(0).getName());
        assertEquals("Project B", result.get(1).getName());
        verify(projectRepository).findByOwnerId(5L);
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

        assertThrows(RuntimeException.class, () -> projectService.deleteProject(99L, 7L));

        verify(projectRepository, never()).delete(any());
    }

    @Test
    void getProjectByIdShouldReturnOwnedProject() {
        User owner = new User();
        owner.setId(5L);

        Project project = new Project("Project Y", "Description Y", owner);
        project.setId(88L);

        when(projectRepository.findById(88L)).thenReturn(java.util.Optional.of(project));

        Project result = projectService.getProjectById(88L, 5L);

        assertEquals(88L, result.getId());
        assertEquals("Project Y", result.getName());
        verify(projectRepository).findById(88L);
    }

    @Test
    void getProjectByIdShouldRejectNonOwner() {
        User owner = new User();
        owner.setId(5L);

        Project project = new Project("Project Y", "Description Y", owner);
        project.setId(88L);

        when(projectRepository.findById(88L)).thenReturn(java.util.Optional.of(project));

        assertThrows(RuntimeException.class, () -> projectService.getProjectById(88L, 9L));
    }

    @Test
    void getProjectByIdShouldThrowWhenNotFound() {
        when(projectRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThrows(RuntimeException.class, () -> projectService.getProjectById(999L, 1L));
    }
}
