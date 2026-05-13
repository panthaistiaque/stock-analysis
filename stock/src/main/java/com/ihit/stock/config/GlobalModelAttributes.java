package com.ihit.stock.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.ihit.stock.dto.MenuItem;
import com.ihit.stock.model.AppUser;
import com.ihit.stock.service.AppUserService;

@ControllerAdvice
public class GlobalModelAttributes {

    @Value("${app.title:Stock Admin 1}")
    private String appTitle;

    private final AppUserService userService;

    public GlobalModelAttributes(AppUserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAuthenticated = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        // ✅ Username (always safe)
        String username = isAuthenticated ? auth.getName() : "Guest";
        model.addAttribute("username", username);
        if (isAuthenticated) {
            AppUser user = userService.findByUsername(username);
            model.addAttribute("user", user);
        }

        // ✅ Role check (never null)
        boolean isAdmin = false;

        if (isAuthenticated) {
            isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }

        model.addAttribute("isAdmin", isAdmin);

        model.addAttribute("appTitle", appTitle);

        // 🔹 Header menus
        List<MenuItem> headerMenus = new ArrayList<>();

        // Common
        headerMenus.add(new MenuItem("Profile", "/user/profile"));
        headerMenus.add(new MenuItem("Change Password", "/user/password"));

        model.addAttribute("headerMenus", headerMenus);

        List<MenuItem> sidebarMenus = new ArrayList<>();

        sidebarMenus.add(new MenuItem("Dashboard", "/dashboard"));
        // sidebarMenus.add(new MenuItem("Upload Excel", "/stocks/upload"));
        sidebarMenus.add(new MenuItem("Market Information", "/stocks/data"));
        sidebarMenus.add(new MenuItem("Market Scraping", "/stocks/market-data-scraping"));
        sidebarMenus.add(new MenuItem("Company Information", "/company/view"));
        sidebarMenus.add(new MenuItem("Listed Companies", "/company/list"));
        sidebarMenus.add(new MenuItem("Trading Codes", "/trading-code"));

        // Admin only
        if (isAdmin) {
            sidebarMenus.add(new MenuItem("User Management", "/admin/users"));
            sidebarMenus.add(new MenuItem("Documentation", "/admin/documentation"));
        }

        model.addAttribute("sidebarMenus", sidebarMenus);

    }
}
