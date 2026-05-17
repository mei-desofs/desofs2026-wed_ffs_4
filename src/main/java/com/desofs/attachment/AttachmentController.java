package com.desofs.attachment;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/api/tasks/{taskId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> listAttachments(@PathVariable UUID taskId) {
        List<AttachmentResponse> attachments = attachmentService
                .listAttachments(taskId, currentUserEmail())
                .stream()
                .map(AttachmentResponse::from)
                .toList();

        return ResponseEntity.ok(attachments);
    }

    @PostMapping(value = "/api/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(@PathVariable UUID taskId,
                                                               @RequestParam("file") MultipartFile file) {
        Attachment attachment = attachmentService.store(taskId, file, currentUserEmail());
        return ResponseEntity.status(201).body(AttachmentResponse.from(attachment));
    }

    @GetMapping("/api/attachments/{id}/download")
    public ResponseEntity<?> downloadAttachment(@PathVariable Long id) {
        AttachmentDownload download = attachmentService.loadForDownload(id, currentUserEmail());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.attachment().getOriginalName())
                        .build()
                        .toString())
                .body(download.resource());
    }

    @DeleteMapping("/api/tasks/{taskId}/attachments/{id}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable UUID taskId, @PathVariable Long id) {
        attachmentService.delete(taskId, id, currentUserEmail());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(Exception ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(Exception ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded() {
        String errorMessage = String.format("File exceeds the maximum size of %s", attachmentService.getMaxFileSize());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", errorMessage));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleStorageErrors(Exception ex) {
        return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        throw new IllegalStateException("No authenticated user in context");
    }
}
