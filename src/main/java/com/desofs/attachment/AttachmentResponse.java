package com.desofs.attachment;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        Long id,
        UUID taskId,
        String fileName,
        long fileSize,
        String mimeType,
        Instant createdAt,
        Long uploadedBy,
        String downloadPath) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getTaskId(),
                attachment.getOriginalName(),
                attachment.getFileSize(),
                attachment.getMimeType(),
                attachment.getCreatedAt(),
                attachment.getUploadedBy(),
                String.format("/api/attachments/%d/download", attachment.getId()));
    }
}
