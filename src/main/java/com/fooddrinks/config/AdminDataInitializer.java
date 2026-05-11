package com.fooddrinks.config;

import com.fooddrinks.entity.Provider;
import com.fooddrinks.entity.Role;
import com.fooddrinks.entity.User;
import com.fooddrinks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Admin credentials can be set via environment variables or application.properties for security.
    @Value("${app.admin.email:admin@foodsdrinks.com}")
    private String adminEmail;

    // Default password can be overridden via environment variable or application.properties for security.
    // In production, ensure to set a strong password via env var and do NOT use the default.
    @Value("${app.admin.password:Admin@123456}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName("Administrator");
            admin.setRole(Role.ADMIN);
            admin.setProvider(Provider.LOCAL);
            admin.setIsActive(true);
            userRepository.save(admin);
        }
    }
}
