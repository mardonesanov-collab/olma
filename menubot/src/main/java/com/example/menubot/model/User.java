package com.example.menubot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** @deprecated Legacy H2 model — SaaS uses {@link com.example.menubot.domain.entity.AppUser} */
// @Entity
// @Table(name = "legacy_users")
public class User {

    @Id
    private Long id;

    private String firstName;
    private String lastName;
    private String username;

    @Enumerated(EnumType.STRING)
    private Language language;

    @Enumerated(EnumType.STRING)
    private UserState state;

    private boolean admin;
    private boolean approved;

    private Long selectedRestaurantId;
    private Long selectedCategoryId;

    private LocalDateTime createdAt;

    public User() {}

    public User(Long id, String firstName, String lastName, String username) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.admin = false;
        this.approved = false;
        this.state = UserState.SELECT_LANGUAGE;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }

    public UserState getState() { return state; }
    public void setState(UserState state) { this.state = state; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public Long getSelectedRestaurantId() { return selectedRestaurantId; }
    public void setSelectedRestaurantId(Long id) { this.selectedRestaurantId = id; }

    public Long getSelectedCategoryId() { return selectedCategoryId; }
    public void setSelectedCategoryId(Long id) { this.selectedCategoryId = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}