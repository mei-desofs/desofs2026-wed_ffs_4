package com.desofs.project;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desofs.user.User;
import com.desofs.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "user@example.com")
    void createProjectShouldReturnCreatedProject() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("user@example.com");

        Project project = new Project("Project 1", "Description", owner);
        project.setId(10L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(owner));
        when(projectService.createProject("Project 1", "Description", owner)).thenReturn(project);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Project 1\",\"description\":\"Description\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("Project 1"))
                .andExpect(jsonPath("$.description").value("Description"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void listProjectsShouldReturnUserProjects() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("user@example.com");

        Project p1 = new Project("Project A", "Description A", owner);
        p1.setId(1L);
        Project p2 = new Project("Project B", "Description B", owner);
        p2.setId(2L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(owner));
        when(projectService.getUserProjects(1L)).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Project A"))
                .andExpect(jsonPath("$[1].name").value("Project B"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void deleteProjectShouldReturnNoContent() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(owner));
        doNothing().when(projectService).deleteProject(10L, 1L);

        mockMvc.perform(delete("/api/projects/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getProjectShouldReturnProjectById() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("user@example.com");

        Project project = new Project("Project Detail", "Full description", owner);
        project.setId(5L);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(owner));
        when(projectService.getProjectById(5L, 1L)).thenReturn(project);

        mockMvc.perform(get("/api/projects/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("Project Detail"))
                .andExpect(jsonPath("$.description").value("Full description"));
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getProjectShouldReturn404WhenNotFound() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(owner));
        when(projectService.getProjectById(999L, 1L)).thenThrow(new RuntimeException("Project not found"));

        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@example.com")
    void getProjectShouldReturn403WhenNotOwner() throws Exception {
        User owner = new User();
        owner.setId(1L);
        owner.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(owner));
        when(projectService.getProjectById(5L, 1L)).thenThrow(new RuntimeException("Forbidden"));

        mockMvc.perform(get("/api/projects/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectEndpointsShouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isForbidden());
    }
}
