package com.desofs.attachment;

import org.springframework.core.io.Resource;

public record AttachmentDownload(
        Attachment attachment,
        Resource resource,
        String contentType
) {
}
