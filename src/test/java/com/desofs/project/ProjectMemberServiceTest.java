package com.desofs.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.desofs.project.model.Project;
import com.desofs.project.repository.ProjectRepository;
import com.desofs.project.service.ProjectMemberService;
import com.desofs.user.User;
import com.desofs.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectMemberService projectMemberService;

    @Test
    void addMemberShouldAllowAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");

        User member = new User();
        member.setId(10L);

        Project project = new Project("Project", "Desc", admin);
        project.setId(100L);

        when(projectRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));
        when(projectRepository.save(project)).thenReturn(project);

        Project result = projectMemberService.addMember(100L, admin, "user@example.com");

        assertEquals(1, result.getMembers().size());
        verify(projectRepository).save(project);
    }

    @Test
    void addMemberShouldAllowManagerWhenMember() {
        User manager = new User();
        manager.setId(2L);
        manager.setRole("MANAGER");

        User member = new User();
        member.setId(10L);

        Project project = new Project("Project", "Desc", manager);
        project.setId(200L);
        project.addMember(manager);

        when(projectRepository.findByIdAndDeletedFalse(200L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));
        when(projectRepository.save(project)).thenReturn(project);

        Project result = projectMemberService.addMember(200L, manager, "user@example.com");

        assertEquals(2, result.getMembers().size());
    }

    @Test
    void addMemberShouldRejectManagerNotMember() {
        User manager = new User();
        manager.setId(2L);
        manager.setRole("MANAGER");

        Project project = new Project("Project", "Desc", manager);
        project.setId(300L);

        when(projectRepository.findByIdAndDeletedFalse(300L)).thenReturn(Optional.of(project));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> projectMemberService.addMember(300L, manager, "user@example.com"));
        assertTrue(ex.getMessage().contains("Forbidden"));
    }

    @Test
    void removeMemberShouldAllowAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");

        User member = new User();
        member.setId(10L);

        Project project = new Project("Project", "Desc", admin);
        project.setId(400L);
        project.addMember(member);

        when(projectRepository.findByIdAndDeletedFalse(400L)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));
        when(projectRepository.save(project)).thenReturn(project);

        Project result = projectMemberService.removeMember(400L, admin, "user@example.com");

        assertEquals(0, result.getMembers().size());
        verify(projectRepository).save(project);
    }
}