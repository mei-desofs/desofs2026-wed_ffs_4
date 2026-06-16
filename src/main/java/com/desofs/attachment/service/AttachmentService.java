package com.desofs.attachment.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.desofs.attachment.model.AttachmentStorageProperties;
import com.desofs.attachment.model.Attachment;
import com.desofs.attachment.model.AttachmentDownload;
import com.desofs.attachment.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.desofs.audit.model.AuditAction;
import com.desofs.audit.service.AuditService;
import com.desofs.project.model.Project;
import com.desofs.project.repository.ProjectRepository;
import com.desofs.task.model.Task;
import com.desofs.task.repository.TaskRepository;
import com.desofs.user.model.User;
import com.desofs.user.repository.UserRepository;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@Service
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final AttachmentStorageProperties properties;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Path storageRoot;

    private static final Map<String, Set<String>> MIME_TYPES_BY_EXTENSION = allowedMimeTypes();
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");

    @Autowired
    public AttachmentService(AttachmentRepository attachmentRepository,
                             AttachmentStorageProperties properties,
                             TaskRepository taskRepository,
                             ProjectRepository projectRepository,
                             UserRepository userRepository,
                             AuditService auditService) {
        this.attachmentRepository = attachmentRepository;
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;

        this.storageRoot = Paths
                .get(properties.getStorageDir())
                .toAbsolutePath().normalize();
    }

    public AttachmentService(AttachmentRepository attachmentRepository,
                             AttachmentStorageProperties properties,
                             TaskRepository taskRepository,
                             ProjectRepository projectRepository,
                             UserRepository userRepository) {
        this(attachmentRepository, properties, taskRepository, projectRepository, userRepository, null);
    }

    @Transactional(readOnly = true)
    public List<Attachment> listAttachments(UUID taskId, String userEmail) {
        User user = resolveUser(userEmail);
        Task task = resolveTask(taskId);
        requireProjectAccess(user, task);
        return attachmentRepository.findByTaskIdAndDeletedAtIsNullOrderByCreatedAtDesc(taskId);
    }

    @Transactional
    public Attachment store(UUID taskId, MultipartFile file, String userEmail) {
        User user = resolveUser(userEmail);
        Task task = resolveTask(taskId);
        requireProjectAccess(user, task);
        enforceUploadRateLimit(user);

        ValidatedFile validatedFile = validateFile(file);

        String uuid = UUID.randomUUID().toString();

        String storedName = String.format("%s.%s", uuid, validatedFile.extension());
        Attachment attachment = attachmentRepository.save(new Attachment(
                validatedFile.originalName(),
                storedName,
                taskId,
                user.getId(),
                file.getSize(),
                validatedFile.mimeType()));

        try {
            Files.createDirectories(storageRoot);
            Path destination = resolveStoragePath(attachment);
            file.transferTo(destination);
            recordAudit(user.getEmail(), AuditAction.ATTACHMENT_UPLOAD, "attachment", String.valueOf(attachment.getId()), true, "Uploaded attachment to task " + taskId);
            return attachment;
        } catch (IOException ex) {
            recordAudit(user.getEmail(), AuditAction.ATTACHMENT_UPLOAD, "attachment", String.valueOf(attachment.getId()), false, ex.getMessage());
            throw new IllegalStateException("Could not store file", ex);
        }
    }

    @Transactional(readOnly = true)
    public AttachmentDownload loadForDownload(Long id, String userEmail) {
        User user = resolveUser(userEmail);
        Attachment attachment = getAttachment(id);
        Task task = resolveTask(attachment.getTaskId());
        requireProjectAccess(user, task);

        Path path = resolveStoragePath(attachment);
        Resource resource = new FileSystemResource(path);

        if (!resource.exists() || !resource.isReadable())
            throw new NoSuchElementException("Attachment file not found");
        recordAudit(user.getEmail(), AuditAction.ATTACHMENT_DOWNLOAD, "attachment", String.valueOf(id), true, "Downloaded attachment");
        return new AttachmentDownload(attachment, resource, attachment.getMimeType());
    }

    @Transactional
    public void delete(UUID taskId, long id, String userEmail) {
        User user = resolveUser(userEmail);
        Attachment attachment = getAttachment(id);
        if (!attachment.getTaskId().equals(taskId))
            throw new NoSuchElementException("Attachment not found for task");

        Task task = resolveTask(taskId);
        requireCanDelete(user, task, attachment);

        Path path = resolveStoragePath(attachment);

        try {
            Files.deleteIfExists(path);
            attachment.setDeletedAt(Instant.now());
            attachmentRepository.save(attachment);
            recordAudit(user.getEmail(), AuditAction.ATTACHMENT_DELETE, "attachment", String.valueOf(id), true, "Deleted attachment");
        } catch (IOException ex) {
            recordAudit(user.getEmail(), AuditAction.ATTACHMENT_DELETE, "attachment", String.valueOf(id), false, ex.getMessage());
            throw new IllegalStateException("Could not delete attachment file", ex);
        }
    }

    private Attachment getAttachment(long id) {
        return attachmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NoSuchElementException("Attachment not found"));
    }

    private ValidatedFile validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File is required");

        if (file.getSize() > properties.getMaxFileSize().toBytes())
            throw new IllegalArgumentException("File exceeds the maximum size of " + properties.getMaxFileSize());

        String originalName = getSafeOriginalName(file);
        String extension = getExtension(originalName);

        if (!normalizedAllowedExtensions().contains(extension))
            throw new IllegalArgumentException("File extension is not allowed");

        String mimeType = normalizedContentType(file);
        Set<String> allowedMimeTypes = MIME_TYPES_BY_EXTENSION.get(extension);
        if (allowedMimeTypes == null || !allowedMimeTypes.contains(mimeType))
            throw new IllegalArgumentException("File MIME type does not match the allowed type for ." + extension);

        validateImagePixelCount(file, extension);

        return new ValidatedFile(originalName, extension, mimeType);
    }

    private void validateImagePixelCount(MultipartFile file, String extension) {
        long maxImagePixels = properties.getMaxImagePixels();
        if (!IMAGE_EXTENSIONS.contains(extension) || maxImagePixels <= 0)
            return;

        try (ImageInputStream imageStream = ImageIO.createImageInputStream(file.getInputStream())) {
            if (imageStream == null)
                throw new IllegalArgumentException("Image file is invalid");

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext())
                throw new IllegalArgumentException("Image file is invalid");

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageStream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixelCount = Math.multiplyExact((long) width, (long) height);
                if (pixelCount > maxImagePixels)
                    throw new IllegalArgumentException("Image dimensions exceed the maximum pixel count of " + maxImagePixels);
            } finally {
                reader.dispose();
            }
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Image dimensions exceed the maximum pixel count of " + maxImagePixels);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Image file is invalid", ex);
        }
    }

    private String getSafeOriginalName(MultipartFile file) {
        String originalName = file.getOriginalFilename();

        if (originalName == null || originalName.isBlank())
            throw new IllegalArgumentException("Original filename is required");

        String safeOriginalName = StringUtils.cleanPath(originalName);

        if (safeOriginalName.contains("/") || safeOriginalName.contains("\\"))
            throw new IllegalArgumentException("Invalid filename");

        if (!StringUtils.hasText(safeOriginalName) || safeOriginalName.contains(".."))
            throw new IllegalArgumentException("Invalid filename");

        return safeOriginalName;
    }

    private String getExtension(String filename) {
        int extensionStart = filename.lastIndexOf('.');
        boolean missingExtension = extensionStart < 0 || extensionStart == filename.length() - 1;

        if (missingExtension)
            throw new IllegalArgumentException("File extension is required");

        String extension = filename.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
        return extension;
    }

    private String normalizedContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank())
            throw new IllegalArgumentException("File MIME type is required");
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        return normalized.split(";", 2)[0].trim();
    }

    private Set<String> normalizedAllowedExtensions() {
        return properties.getAllowedExtensions().stream()
                .map(extension -> extension.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Path resolveStoragePath(Attachment attachment) {
        String fileName = attachment.getStoredName();
        Path destination = storageRoot.resolve(fileName).normalize();
        if (!destination.startsWith(storageRoot))
            throw new IllegalStateException("Invalid storage path");
        return destination;
    }

    public String getMaxFileSize() {
        return properties.getMaxFileSize().toString();
    }

    private void enforceUploadRateLimit(User user) {
        if (properties.getMaxUploadsPerWindow() <= 0)
            return;

        Instant windowStart = Instant.now().minus(properties.getUploadWindow());
        long uploadsInWindow = attachmentRepository.countByUploadedByAndCreatedAtAfter(user.getId(), windowStart);
        if (uploadsInWindow >= properties.getMaxUploadsPerWindow())
            throw new IllegalArgumentException("Upload rate limit exceeded");
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    private Task resolveTask(UUID taskId) {
        return taskRepository.findByIdAndDeletedFalse(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found"));
    }

    private Project resolveProject(Task task) {
        return projectRepository.findByIdAndDeletedFalse(task.getProjectId())
                .orElseThrow(() -> new NoSuchElementException("Project not found"));
    }

    private void requireProjectAccess(User user, Task task) {
        Project project = resolveProject(task);
        if (isAdmin(user))
            return;

        boolean isMember = project.getMembers() != null && project.getMembers().stream()
                .anyMatch(member -> member.getId() != null && member.getId().equals(user.getId()));
        if (!isMember)
            throw new AccessDeniedException("Forbidden");
    }

    private void requireCanDelete(User user, Task task, Attachment attachment) {
        requireProjectAccess(user, task);
        if (isAdmin(user) || attachment.getUploadedBy().equals(user.getId()) || "MANAGER".equals(user.getRole()))
            return;

        throw new AccessDeniedException("Forbidden");
    }

    private boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }

    private void recordAudit(String actor, AuditAction action, String resourceType, String resourceId, boolean success, String details) {
        if (auditService != null) {
            auditService.record(actor, action, resourceType, resourceId, success, details);
        }
    }

    private static Map<String, Set<String>> allowedMimeTypes() {
        Map<String, Set<String>> mimeTypes = new HashMap<>();
        mimeTypes.put("pdf", Set.of("application/pdf"));
        mimeTypes.put("doc", Set.of("application/msword"));
        mimeTypes.put("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        mimeTypes.put("xls", Set.of("application/vnd.ms-excel"));
        mimeTypes.put("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        mimeTypes.put("ppt", Set.of("application/vnd.ms-powerpoint"));
        mimeTypes.put("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation"));
        mimeTypes.put("jpg", Set.of("image/jpeg"));
        mimeTypes.put("jpeg", Set.of("image/jpeg"));
        mimeTypes.put("png", Set.of("image/png"));
        mimeTypes.put("gif", Set.of("image/gif"));
        return Map.copyOf(mimeTypes);
    }

    private record ValidatedFile(String originalName, String extension, String mimeType) {}
}
