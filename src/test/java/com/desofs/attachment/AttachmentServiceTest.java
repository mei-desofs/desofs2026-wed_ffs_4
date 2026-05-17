package com.desofs.attachment;

import com.desofs.project.model.Project;
import com.desofs.project.repository.ProjectRepository;
import com.desofs.task.Task;
import com.desofs.task.TaskRepository;
import com.desofs.user.User;
import com.desofs.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {
    @TempDir
    Path storageDir;

    @Mock private AttachmentRepository attachmentRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;

    private AttachmentService service;
    private AttachmentStorageProperties properties;

    private static final UUID TASK_ID = UUID.randomUUID();
    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 20L;
    private static final String USER_EMAIL = "user@example.com";

    private User user;
    private Task task;
    private Project project;

    @BeforeEach
    void setUp() {
        properties = new AttachmentStorageProperties();
        properties.setStorageDir(storageDir.toString());
        properties.setAllowedExtensions(Set.of("pdf", "png"));
        properties.setMaxUploadsPerWindow(10);
        properties.setUploadWindow(Duration.ofHours(1));

        service = new AttachmentService(
                attachmentRepository,
                properties,
                taskRepository,
                projectRepository,
                userRepository);

        user = user(USER_ID, USER_EMAIL, "USER");
        task = new Task();
        task.setProjectId(PROJECT_ID);
        task.setTitle("Task");
        task.setCreatedBy(USER_ID);
        project = new Project("Project", "Description", user);
        project.addMember(user);
    }

    @Test
    void store_whenProjectMemberAndValidFile_savesTaskScopedAttachment() {
        stubAccess(user, task, project);
        when(attachmentRepository.countByUploadedByAndCreatedAtAfter(eq(USER_ID), any())).thenReturn(0L);
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "%PDF-1.4".getBytes());

        Attachment attachment = service.store(TASK_ID, file, USER_EMAIL);

        assertEquals(TASK_ID, attachment.getTaskId());
        assertEquals(USER_ID, attachment.getUploadedBy());
        assertEquals("report.pdf", attachment.getOriginalName());
        assertEquals("application/pdf", attachment.getMimeType());
        assertEquals(file.getSize(), attachment.getFileSize());
        assertTrue(Files.exists(storageDir.resolve(attachment.getStoredName())));
    }

    @Test
    void store_whenMimeDoesNotMatchExtension_rejectsFile() {
        stubAccess(user, task, project);
        when(attachmentRepository.countByUploadedByAndCreatedAtAfter(eq(USER_ID), any())).thenReturn(0L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "image/png", "not a pdf".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.store(TASK_ID, file, USER_EMAIL));

        assertTrue(ex.getMessage().contains("MIME"));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void store_whenUploadLimitReached_rejectsFile() {
        properties.setMaxUploadsPerWindow(1);
        stubAccess(user, task, project);
        when(attachmentRepository.countByUploadedByAndCreatedAtAfter(eq(USER_ID), any())).thenReturn(1L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "%PDF-1.4".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.store(TASK_ID, file, USER_EMAIL));

        assertEquals("Upload rate limit exceeded", ex.getMessage());
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void listAttachments_whenUserIsNotProjectMember_isForbidden() {
        Project emptyProject = new Project("Project", "Description", user);
        stubAccess(user, task, emptyProject);

        assertThrows(AccessDeniedException.class,
                () -> service.listAttachments(TASK_ID, USER_EMAIL));
    }

    @Test
    void loadForDownload_whenMember_returnsStoredResource() throws Exception {
        Attachment attachment = attachment("report.pdf", "stored.pdf", USER_ID);
        Files.writeString(storageDir.resolve("stored.pdf"), "file");
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
        when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachment.getId()))
                .thenReturn(Optional.of(attachment));

        AttachmentDownload download = service.loadForDownload(attachment.getId(), USER_EMAIL);

        assertEquals("application/pdf", download.contentType());
        assertTrue(download.resource().exists());
    }

    @Test
    void delete_whenUploader_removesFileAndSoftDeletesMetadata() throws Exception {
        Attachment attachment = attachment("report.pdf", "stored.pdf", USER_ID);
        Files.writeString(storageDir.resolve("stored.pdf"), "file");
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(task));
        when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(project));
        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachment.getId()))
                .thenReturn(Optional.of(attachment));

        service.delete(TASK_ID, attachment.getId(), USER_EMAIL);

        assertFalse(Files.exists(storageDir.resolve("stored.pdf")));
        ArgumentCaptor<Attachment> captor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(captor.capture());
        assertNotNull(captor.getValue().getDeletedAt());
    }

    @Test
    void delete_whenAttachmentBelongsToDifferentTask_returnsNotFound() {
        Attachment attachment = attachment("report.pdf", "stored.pdf", USER_ID);
        UUID otherTaskId = UUID.randomUUID();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(user));
        when(attachmentRepository.findByIdAndDeletedAtIsNull(attachment.getId()))
                .thenReturn(Optional.of(attachment));

        assertThrows(NoSuchElementException.class,
                () -> service.delete(otherTaskId, attachment.getId(), USER_EMAIL));
    }

    private void stubAccess(User actor, Task accessibleTask, Project accessibleProject) {
        when(userRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(taskRepository.findByIdAndDeletedFalse(TASK_ID)).thenReturn(Optional.of(accessibleTask));
        when(projectRepository.findByIdAndDeletedFalse(PROJECT_ID)).thenReturn(Optional.of(accessibleProject));
    }

    private Attachment attachment(String originalName, String storedName, Long uploadedBy) {
        Attachment attachment = new Attachment(originalName, storedName, TASK_ID, uploadedBy, 4L, "application/pdf");
        attachment.setId(1L);
        return attachment;
    }

    private User user(Long id, String email, String role) {
        User result = new User();
        result.setId(id);
        result.setEmail(email);
        result.setRole(role);
        result.setPassword("password");
        return result;
    }
}
