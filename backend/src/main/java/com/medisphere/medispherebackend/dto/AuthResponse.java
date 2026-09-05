package com.medisphere.medispherebackend.dto;

import com.medisphere.medispherebackend.model.Role;

import java.time.LocalDateTime;

public class AuthResponse {

    private String id;
    private String fullName;
    private String username;
    private Role role;
    private LocalDateTime createdAt;

    public AuthResponse() {
    }

    public AuthResponse(String id, String fullName, String username, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
