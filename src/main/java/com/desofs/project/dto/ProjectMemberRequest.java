package com.desofs.project.dto;

public class ProjectMemberRequest {
    private String email;

    public ProjectMemberRequest() {}

    public ProjectMemberRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}