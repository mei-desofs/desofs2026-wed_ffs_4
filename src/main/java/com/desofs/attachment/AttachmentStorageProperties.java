package com.desofs.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "app.attachments")
public class AttachmentStorageProperties {
    private String storageDir = "uploads/attachments";
    private DataSize maxFileSize = DataSize.ofMegabytes(25);
    private Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "pdf");

    public String getStorageDir() { return storageDir; }
    public void setStorageDir(String storageDir) { this.storageDir = storageDir; }
    public DataSize getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(DataSize maxFileSize) { this.maxFileSize = maxFileSize; }
    public Set<String> getAllowedExtensions() { return Collections.unmodifiableSet(allowedExtensions); }
    public void setAllowedExtensions(Set<String> allowedExtensions) { this.allowedExtensions = new HashSet<>(allowedExtensions); }
}
