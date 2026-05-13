package com.ihit.stock.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean approved;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String phoneNumber;

    private String gender;

    @Column(length = 1000)
    private String address;

    @Column(length = 1000)
    private String profilePictureUrl;

    @Column(length = 1000)
    private String websiteSocialLinks;

    protected AppUser() {
    }

    public AppUser(String fullName, String email, String username, String password, String role, boolean approved) {
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.password = password;
        this.role = role;
        this.approved = approved;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }

    public boolean isApproved() {
        return approved;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public String getAddress() {
        return address;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public String getWebsiteSocialLinks() {
        return websiteSocialLinks;
    }

    public void updateProfile(String fullName, String email, String phoneNumber, String gender,
            String address, String profilePictureUrl, String websiteSocialLinks) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.address = address;
        this.profilePictureUrl = profilePictureUrl;
        this.websiteSocialLinks = websiteSocialLinks;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void approve() {
        this.approved = true;
    }
}
