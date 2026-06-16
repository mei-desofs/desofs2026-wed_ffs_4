package com.desofs.project.dto;

import com.desofs.project.model.Project;

public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private boolean deleted;

    public static ProjectResponse from(Project project) {
        ProjectResponse r = new ProjectResponse();
        r.id = project.getId();
        r.name = project.getName();
        r.description = project.getDescription();
        r.deleted = project.isDeleted();
        return r;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isDeleted() { return deleted; }
}