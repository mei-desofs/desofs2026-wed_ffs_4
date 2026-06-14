package com.desofs.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuditService}.
 * Tests the audit event recording functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService")
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditService auditService;

    @Nested
    @DisplayName("record method")
    class RecordMethod {

        @Test
        @DisplayName("saves audit event with all parameters provided")
        void record_allParameters_saves() {
            auditService.record("user@example.com", AuditAction.PROJECT_CREATE, "project", "123", true, "Project created");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("saves audit event when actor is null and converts to 'anonymous'")
        void record_nullActor_convertsToAnonymous() {
            auditService.record(null, AuditAction.PROJECT_READ, "project", "456", true, "Project read");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("saves audit event when actor is blank and converts to 'anonymous'")
        void record_blankActor_convertsToAnonymous() {
            auditService.record("   ", AuditAction.PROJECT_READ, "project", "789", true, "Project read");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("saves audit event when resourceId is null and converts to '-'")
        void record_nullResourceId_convertsToDash() {
            auditService.record("user@example.com", AuditAction.PROJECT_DELETE, "project", null, true, "Project deleted");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("saves audit event with details parameter")
        void record_withDetails_saves() {
            auditService.record("manager@example.com", AuditAction.PROJECT_UPDATE, "project", "999", true, "Project fields updated");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("saves audit event even if repository throws exception")
        void record_repositoryThrowsException_logError() {
            when(auditEventRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            // Should not throw - exception is caught and logged
            auditService.record("user@example.com", AuditAction.PROJECT_CREATE, "project", "100", true, "Test");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("saves audit event with success=false for failed actions")
        void record_unsuccessfulAction_saves() {
            auditService.record("user@example.com", AuditAction.PROJECT_DELETE, "project", "200", false, "Deletion failed");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("saves audit event with action PROJECT_CREATE")
        void record_projectCreate_saves() {
            auditService.record("owner@example.com", AuditAction.PROJECT_CREATE, "project", "1", true, "");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("saves audit event with action PROJECT_READ")
        void record_projectRead_saves() {
            auditService.record("viewer@example.com", AuditAction.PROJECT_READ, "project", "2", true, "");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("saves audit event with action PROJECT_UPDATE")
        void record_projectUpdate_saves() {
            auditService.record("editor@example.com", AuditAction.PROJECT_UPDATE, "project", "3", true, "");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("saves audit event with action PROJECT_DELETE")
        void record_projectDelete_saves() {
            auditService.record("admin@example.com", AuditAction.PROJECT_DELETE, "project", "4", true, "");

            verify(auditEventRepository).save(any(AuditEvent.class));
        }
    }
}

