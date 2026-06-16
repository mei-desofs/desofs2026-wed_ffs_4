package com.desofs.audit.service;

import com.desofs.audit.model.AuditAction;
import com.desofs.audit.model.AuditEvent;
import com.desofs.audit.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(String actor, AuditAction action, String resourceType, String resourceId, boolean success, String details) {
        try {
            auditEventRepository.save(new AuditEvent(
                    actor == null || actor.isBlank() ? "anonymous" : actor,
                    action,
                    resourceType,
                    resourceId == null ? "-" : resourceId,
                    success,
                    details));
        } catch (Exception ex) {
            log.warn("Unable to persist audit event {} for {}:{}", action, resourceType, resourceId, ex);
        }
    }
}