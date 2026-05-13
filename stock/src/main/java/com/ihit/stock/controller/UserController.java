package com.ihit.stock.controller;

import com.ihit.stock.dto.PasswordChangeForm;
import com.ihit.stock.dto.ProfileForm;
import com.ihit.stock.service.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
public class UserController {

    private final AppUserService userService;

    public UserController(AppUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Authentication authentication, Model model) {
        // addShellAttributes(principal, authentication, model);
        model.addAttribute("profileForm", userService.getProfileForm(principal.getName()));
        return "profile";
    }

    @GetMapping("/password")
    public String password(Principal principal, Authentication authentication, Model model) {
        // addShellAttributes(principal, authentication, model);
        model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        return "password";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute ProfileForm profileForm, Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfile(principal.getName(), profileForm);
            redirectAttributes.addFlashAttribute("profileSuccess", "Profile updated.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("profileError", exception.getMessage());
        }
        return "redirect:/user/profile";
    }

    @PostMapping("/password")
    public String changePassword(@ModelAttribute PasswordChangeForm passwordChangeForm, Principal principal,
                                 Authentication authentication, HttpServletRequest request,
                                 HttpServletResponse response, RedirectAttributes redirectAttributes) {
        try {
            userService.changePassword(principal.getName(), passwordChangeForm);
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            return "redirect:/login?passwordChanged";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("passwordError", exception.getMessage());
        }
        return "redirect:/user/password";
    }

    
}
