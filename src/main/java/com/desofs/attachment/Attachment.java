package com.desofs.attachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachments")
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "original_name")
    private String originalName;

    @Column(nullable = false, unique = true, name = "stored_name")
    private String storedName;

    @Column(nullable = false, name = "task_id")
    private UUID taskId;

    @Column(nullable = false, name = "uploaded_by")
    private Long uploadedBy;

    @Column(nullable = false, name = "file_size")
    private long fileSize;

    @Column(nullable = false, name = "mime_type")
    private String mimeType;

    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Attachment() {}

    public Attachment(String originalName, String storedName, UUID taskId, Long uploadedBy, long fileSize, String mimeType) {
        this.originalName = originalName;
        this.storedName = storedName;
        this.taskId = taskId;
        this.uploadedBy = uploadedBy;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    @PrePersist
    void setCreatedAtOnCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }
    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }
    public Long getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
