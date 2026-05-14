package com.desofs.attachment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorageProperties properties;
    private final Path storageRoot;

    public AttachmentService(AttachmentRepository attachmentRepository, AttachmentStorageProperties properties) {
        this.attachmentRepository = attachmentRepository;
        this.properties = properties;

        this.storageRoot = Paths
                .get(properties.getStorageDir())
                .toAbsolutePath().normalize();
    }

    public List<Attachment> listAttachments() {
        return attachmentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public Attachment store(MultipartFile file) {
        validateFile(file);

        String originalName = getSafeOriginalName(file);
        String extension = getExtension(originalName);
        String uuid = UUID.randomUUID().toString();

        String storedName = String.format("%s.%s", uuid, extension);
        Attachment attachment = attachmentRepository.save(new Attachment(originalName, storedName));

        try {
            Files.createDirectories(storageRoot);
            Path destination = resolveStoragePath(attachment);
            file.transferTo(destination);
            return attachment;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store file", ex);
        }
    }

    public AttachmentDownload loadForDownload(Long id) {
        Attachment attachment = getAttachment(id);
        Path path = resolveStoragePath(attachment);
        Resource resource = new FileSystemResource(path);

        if (!resource.exists() || !resource.isReadable())
            throw new NoSuchElementException("Attachment file not found");

        try {
            String contentType = Files.probeContentType(path);
            contentType = contentType != null ? contentType : "application/octet-stream";
            return new AttachmentDownload(attachment, resource, contentType);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read attachment file", ex);
        }
    }

    @Transactional
    public void delete(long id) {
        Attachment attachment = getAttachment(id);
        Path path = resolveStoragePath(attachment);

        try {
            Files.deleteIfExists(path);
            attachmentRepository.delete(attachment);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete attachment file", ex);
        }
    }

    private Attachment getAttachment(long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Attachment not found"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File is required");

        if (file.getSize() > properties.getMaxFileSize().toBytes())
            throw new IllegalArgumentException("File exceeds the maximum size of " + properties.getMaxFileSize());

        String originalName = getSafeOriginalName(file);
        String extension = getExtension(originalName);

        if (!normalizedAllowedExtensions().contains(extension))
            throw new IllegalArgumentException("File extension is not allowed");
    }

    private String getSafeOriginalName(MultipartFile file) {
        String originalName = file.getOriginalFilename();

        if (!StringUtils.hasText(originalName))
            throw new IllegalArgumentException("Original filename is required");

        String normalizedName = originalName.replace("\\", "/");
        String filename = StringUtils.getFilename(StringUtils.cleanPath(normalizedName));

        if (!StringUtils.hasText(filename) || filename.contains(".."))
            throw new IllegalArgumentException("Invalid filename");

        return filename;
    }

    private String getExtension(String filename) {
        int extensionStart = filename.lastIndexOf('.');
        boolean hasExtension = extensionStart < 0 || extensionStart == filename.length() - 1;

        if (!hasExtension)
            throw new IllegalArgumentException("File extension is required");

        String extension = filename.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
        return extension;
    }

    private Set<String> normalizedAllowedExtensions() {
        return properties.getAllowedExtensions().stream()
                .map(extension -> extension.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Path resolveStoragePath(Attachment attachment) {
        String fileName = attachment.getStoredName();
        Path destination = storageRoot.resolve(fileName).normalize();
        return destination;
    }

    public String getMaxFileSize() {
        return properties.getMaxFileSize().toString();
    }
}
