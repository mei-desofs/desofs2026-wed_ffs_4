package com.desofs.audit.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private String resourceId;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    public AuditEvent() {}

    public AuditEvent(String actor, AuditAction action, String resourceType, String resourceId, boolean success, String details) {
        this.actor = actor;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.success = success;
        this.details = details;
    }

    public Long getId() { return id; }
    public String getActor() { return actor; }
    public AuditAction getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public boolean isSuccess() { return success; }
    public String getDetails() { return details; }
    public Instant getOccurredAt() { return occurredAt; }
}
