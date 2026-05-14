package com.desofs.attachment;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        String fileName,
        Instant createdAt,
        String downloadPath) {
    public static AttachmentResponse from(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getOriginalName(),
                attachment.getCreatedAt(),
                String.format("/api/attachments/%d/download", attachment.getId()));
    }
}
