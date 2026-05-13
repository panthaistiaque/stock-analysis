package com.ihit.stock.config;

import com.ihit.stock.service.AppUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserService userService;

    public DataInitializer(AppUserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        userService.createDefaultAdmin();
    }
}
