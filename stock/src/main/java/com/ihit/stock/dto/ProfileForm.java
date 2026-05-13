package com.ihit.stock.dto;

public class ProfileForm {

    private String fullName;
    private String email;
    private String phoneNumber;
    private String gender;
    private String address;
    private String profilePictureUrl;
    private String websiteSocialLinks;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getWebsiteSocialLinks() {
        return websiteSocialLinks;
    }

    public void setWebsiteSocialLinks(String websiteSocialLinks) {
        this.websiteSocialLinks = websiteSocialLinks;
    }
}
