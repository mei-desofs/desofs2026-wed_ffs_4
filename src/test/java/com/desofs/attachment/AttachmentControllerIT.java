package com.desofs.attachment;

import com.desofs.attachment.model.Attachment;
import com.desofs.attachment.repository.AttachmentRepository;
import com.desofs.audit.repository.AuditEventRepository;
import com.desofs.project.model.Project;
import com.desofs.project.repository.ProjectRepository;
import com.desofs.task.model.Task;
import com.desofs.task.repository.TaskRepository;
import com.desofs.user.model.User;
import com.desofs.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.ApplicationEventsTestExecutionListener;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestExecutionListeners(
        listeners = {
                ServletTestExecutionListener.class,
                DirtiesContextBeforeModesTestExecutionListener.class,
                ApplicationEventsTestExecutionListener.class,
                DependencyInjectionTestExecutionListener.class,
                DirtiesContextTestExecutionListener.class,
                EventPublishingTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
class AttachmentControllerIT {
    private static final Path STORAGE_DIR = Path.of(
            System.getProperty("java.io.tmpdir"),
            "desofs-attachment-it-" + UUID.randomUUID());

    private static final byte[] PDF_BYTES = "%PDF-1.4\nattachment".getBytes();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AttachmentRepository attachmentRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditEventRepository auditEventRepository;

    @DynamicPropertySource
    static void attachmentProperties(DynamicPropertyRegistry registry) {
        registry.add("app.attachments.storage-dir", STORAGE_DIR::toString);
        registry.add("app.attachments.max-file-size", () -> "1MB");
        registry.add("app.attachments.max-uploads-per-window", () -> "10");
    }

    @BeforeEach
    void resetState() throws IOException {
        attachmentRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        auditEventRepository.deleteAll();
        deleteStorageDirectory();
        Files.createDirectories(STORAGE_DIR);
    }

    @AfterAll
    static void cleanStorageDirectory() throws IOException {
        deleteStorageDirectory();
    }

    @Test
    void memberCanUploadListDownloadAndDeleteOwnAttachment() throws Exception {
        TestData data = createTestData();

        Long attachmentId = uploadPdf(data.task(), data.uploader())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(data.task().getId().toString()))
                .andExpect(jsonPath("$.fileName").value("report.pdf"))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.uploadedBy").value(data.uploader().getId()))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(this::attachmentIdFrom);

        Attachment savedAttachment = attachmentRepository.findById(attachmentId).orElseThrow();
        Path storedPath = STORAGE_DIR.resolve(savedAttachment.getStoredName());
        assertTrue(Files.exists(storedPath));

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", data.task().getId())
                        .with(user(data.uploader().getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(attachmentId))
                .andExpect(jsonPath("$[0].fileName").value("report.pdf"))
                .andExpect(jsonPath("$[0].downloadPath").value("/api/attachments/" + attachmentId + "/download"));

        mockMvc.perform(get("/api/attachments/{id}/download", attachmentId)
                        .with(user(data.uploader().getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report.pdf\""))
                .andExpect(content().bytes(PDF_BYTES));

        mockMvc.perform(delete("/api/tasks/{taskId}/attachments/{id}", data.task().getId(), attachmentId)
                        .with(user(data.uploader().getEmail()).roles("USER")))
                .andExpect(status().isNoContent());

        assertFalse(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId).isPresent());
        assertNotNull(attachmentRepository.findById(attachmentId).orElseThrow().getDeletedAt());
        assertFalse(Files.exists(storedPath));
    }

    @Test
    void attachmentEndpointsRejectUnauthenticatedRequests() throws Exception {
        UUID taskId = UUID.randomUUID();

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", taskId))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", taskId)
                        .file(pdfFile("report.pdf")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/attachments/{id}/download", 1L))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/tasks/{taskId}/attachments/{id}", taskId, 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonProjectMemberCannotUploadListOrDownloadAttachments() throws Exception {
        TestData data = createTestData();
        Long attachmentId = persistedAttachment(data.task(), data.uploader());

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", data.task().getId())
                        .file(pdfFile("report.pdf"))
                        .with(user(data.outsider().getEmail()).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tasks/{taskId}/attachments", data.task().getId())
                        .with(user(data.outsider().getEmail()).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/attachments/{id}/download", attachmentId)
                        .with(user(data.outsider().getEmail()).roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadRejectsMimeMismatchWithoutPersistingAttachment() throws Exception {
        TestData data = createTestData();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                MediaType.IMAGE_PNG_VALUE,
                "not a pdf".getBytes());

        mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", data.task().getId())
                        .file(file)
                        .with(user(data.uploader().getEmail()).roles("USER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File MIME type does not match the allowed type for .pdf"));

        assertTrue(attachmentRepository.findAll().isEmpty());
        assertStorageDirectoryEmpty();
    }

    @Test
    void projectMemberCannotDeleteAnotherUsersAttachmentButManagerCan() throws Exception {
        TestData data = createTestData();
        Long attachmentId = uploadPdf(data.task(), data.uploader())
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .transform(this::attachmentIdFrom);

        mockMvc.perform(delete("/api/tasks/{taskId}/attachments/{id}", data.task().getId(), attachmentId)
                        .with(user(data.teammate().getEmail()).roles("USER")))
                .andExpect(status().isForbidden());

        assertTrue(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId).isPresent());

        mockMvc.perform(delete("/api/tasks/{taskId}/attachments/{id}", data.task().getId(), attachmentId)
                        .with(user(data.manager().getEmail()).roles("MANAGER")))
                .andExpect(status().isNoContent());

        assertFalse(attachmentRepository.findByIdAndDeletedAtIsNull(attachmentId).isPresent());
    }

    private ResultActions uploadPdf(Task task, User uploader) throws Exception {
        return mockMvc.perform(multipart("/api/tasks/{taskId}/attachments", task.getId())
                .file(pdfFile("report.pdf"))
                .with(user(uploader.getEmail()).roles("USER")));
    }

    private MockMultipartFile pdfFile(String filename) {
        return new MockMultipartFile("file", filename, MediaType.APPLICATION_PDF_VALUE, PDF_BYTES);
    }

    private Long attachmentIdFrom(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            return json.get("id").asLong();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not parse attachment response", ex);
        }
    }

    private Long persistedAttachment(Task task, User uploader) throws IOException {
        Attachment attachment = attachmentRepository.save(new Attachment(
                "report.pdf",
                UUID.randomUUID() + ".pdf",
                task.getId(),
                uploader.getId(),
                PDF_BYTES.length,
                MediaType.APPLICATION_PDF_VALUE));
        Files.write(STORAGE_DIR.resolve(attachment.getStoredName()), PDF_BYTES);
        return attachment.getId();
    }

    private TestData createTestData() {
        User admin = userRepository.save(testUser("admin@example.com", "ADMIN"));
        User uploader = userRepository.save(testUser("uploader@example.com", "USER"));
        User teammate = userRepository.save(testUser("teammate@example.com", "USER"));
        User manager = userRepository.save(testUser("manager@example.com", "MANAGER"));
        User outsider = userRepository.save(testUser("outsider@example.com", "USER"));

        Project project = new Project("Attachments", "Attachment integration test project", admin);
        project.addMember(uploader);
        project.addMember(teammate);
        project.addMember(manager);
        project = projectRepository.save(project);

        Task task = new Task();
        task.setProjectId(project.getId());
        task.setTitle("Task with attachments");
        task.setCreatedBy(uploader.getId());
        task = taskRepository.save(task);

        return new TestData(uploader, teammate, manager, outsider, task);
    }

    private User testUser(String email, String role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("{noop}password");
        user.setRole(role);
        return user;
    }

    private void assertStorageDirectoryEmpty() throws IOException {
        try (var paths = Files.list(STORAGE_DIR)) {
            assertTrue(paths.findAny().isEmpty());
        }
    }

    private static void deleteStorageDirectory() throws IOException {
        if (!Files.exists(STORAGE_DIR)) {
            return;
        }

        try (var paths = Files.walk(STORAGE_DIR)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            throw new IllegalStateException("Could not delete " + path, ex);
                        }
                    });
        }
    }

    private record TestData(User uploader, User teammate, User manager, User outsider, Task task) {}
}
