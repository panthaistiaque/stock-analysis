package com.ihit.stock.service;

import com.ihit.stock.dto.PasswordChangeForm;
import com.ihit.stock.dto.ProfileForm;
import com.ihit.stock.model.AppUser;
import com.ihit.stock.repository.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Transactional
    public void registerPendingUser(String fullName, String email,String username, String password) { 
        String cleanFullName = cleanOptional(fullName);
        String cleanEmail = requireValue(email, "email");
        String cleanUsername = requireValue(username, "Username");
        String cleanPassword = requireValue(password, "Password");

         if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsernameIgnoreCase(cleanUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        userRepository.save(new AppUser(cleanFullName, cleanEmail, cleanUsername, passwordEncoder.encode(cleanPassword), "USER", false));
    }

    @Transactional
    public void createDefaultAdmin() {
        if (!userRepository.existsByUsernameIgnoreCase("admin")) {
            userRepository.save(new AppUser("", "", "admin", passwordEncoder.encode("admin123"), "ADMIN", true));
        }
    }

    @Transactional
    public void approveUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.approve();
    }

    @Transactional(readOnly = true)
    public Page<AppUser> findAll(String search, String roleFilter, Pageable pageable) {
        if ((search != null && !search.isBlank()) || (roleFilter != null && !roleFilter.isBlank())) {
            return userRepository.findAllFiltered(search, roleFilter, pageable);
        }
        return userRepository.findAll(pageable);
    }

    public List<AppUser> findAllUsers() {
        return userRepository.findAllByOrderByIdAsc();
    }

    public boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public AppUser findByUsername(String username) {
        return findUserByUsername(username);
    }

    public ProfileForm getProfileForm(String username) {
        AppUser user = findUserByUsername(username);
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhoneNumber(user.getPhoneNumber());
        form.setGender(user.getGender());
        form.setAddress(user.getAddress());
        form.setProfilePictureUrl(user.getProfilePictureUrl());
        form.setWebsiteSocialLinks(user.getWebsiteSocialLinks());
        return form;
    }

    @Transactional
    public void updateProfile(String username, ProfileForm form) {
        AppUser user = findUserByUsername(username);
        String cleanEmail = cleanOptional(form.getEmail());

        if (cleanEmail != null && userRepository.existsByEmailIgnoreCaseAndIdNot(cleanEmail, user.getId())) {
            throw new IllegalArgumentException("Email address already exists");
        }

        user.updateProfile(
                cleanOptional(form.getFullName()),
                cleanEmail,
                cleanOptional(form.getPhoneNumber()),
                cleanOptional(form.getGender()),
                cleanOptional(form.getAddress()),
                cleanOptional(form.getProfilePictureUrl()),
                cleanOptional(form.getWebsiteSocialLinks()));
    }

    @Transactional
    public void changePassword(String username, PasswordChangeForm form) {
        AppUser user = findUserByUsername(username);

        String oldPassword = requireValue(form.getOldPassword(), "Old password");
        String newPassword = requireValue(form.getNewPassword(), "New password");
        String confirmPassword = requireValue(form.getConfirmPassword(), "Confirm password");

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = findUserByUsername(username);

        return User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .disabled(!user.isApproved())
                .build();
    }

    private AppUser findUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private String cleanOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String requireValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
