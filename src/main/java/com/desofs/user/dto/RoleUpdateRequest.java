package com.desofs.user.dto;

public class RoleUpdateRequest {
    private String role;

    public RoleUpdateRequest() {}

    public RoleUpdateRequest(String role) {
        this.role = role;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
