package com.desofs.attachment;

import com.desofs.attachment.controller.AttachmentController;
import com.desofs.attachment.model.Attachment;
import com.desofs.attachment.model.AttachmentDownload;
import com.desofs.attachment.model.AttachmentResponse;
import com.desofs.attachment.service.AttachmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {
    @Mock
    private AttachmentService attachmentService;

    @InjectMocks
    private AttachmentController controller;

    private static final UUID TASK_ID = UUID.randomUUID();
    private static final String USER_EMAIL = "user@example.com";

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listAttachments_usesTaskScopeAndCurrentUser() {
        setAuthenticatedUser();
        when(attachmentService.listAttachments(TASK_ID, USER_EMAIL)).thenReturn(List.of(attachment()));

        ResponseEntity<List<AttachmentResponse>> response = controller.listAttachments(TASK_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(attachmentService).listAttachments(TASK_ID, USER_EMAIL);
    }

    @Test
    void uploadAttachment_usesTaskScopeAndCurrentUser() {
        setAuthenticatedUser();
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "%PDF".getBytes());
        Attachment attachment = attachment();
        when(attachmentService.store(TASK_ID, file, USER_EMAIL)).thenReturn(attachment);

        ResponseEntity<AttachmentResponse> response = controller.uploadAttachment(TASK_ID, file);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(attachment.getId(), response.getBody().id());
    }

    @Test
    void downloadAttachment_setsContentDispositionFilename() {
        setAuthenticatedUser();
        Attachment attachment = attachment();
        AttachmentDownload download = new AttachmentDownload(
                attachment,
                new ByteArrayResource("file".getBytes()),
                "application/pdf");
        when(attachmentService.loadForDownload(attachment.getId(), USER_EMAIL)).thenReturn(download);

        ResponseEntity<?> response = controller.downloadAttachment(attachment.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("report.pdf"));
    }

    @Test
    void deleteAttachment_usesTaskScopeAndCurrentUser() {
        setAuthenticatedUser();

        ResponseEntity<Void> response = controller.deleteAttachment(TASK_ID, 1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(attachmentService).delete(TASK_ID, 1L, USER_EMAIL);
    }

    private void setAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_EMAIL, null, List.of()));
    }

    private Attachment attachment() {
        Attachment attachment = new Attachment("report.pdf", "stored.pdf", TASK_ID, 1L, 4L, "application/pdf");
        attachment.setId(1L);
        return attachment;
    }
}
