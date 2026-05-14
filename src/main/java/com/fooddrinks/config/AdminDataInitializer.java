package com.fooddrinks.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fooddrinks.entity.Provider;
import com.fooddrinks.entity.Role;
import com.fooddrinks.entity.User;
import com.fooddrinks.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Opt-in flag — admin bootstrapping only runs when explicitly enabled.
    // Set app.admin.create=true in application-dev.properties or via env var.
    // Do NOT enable in production without also setting strong credentials.
    @Value("${app.admin.create:false}")
    private boolean createEnabled;

    // Admin credentials must be provided via environment variables or
    // application.properties.
    // There are no static defaults — the app will not create an admin unless these
    // are set.
    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (!createEnabled)
            return;

        Optional<User> existing = userRepository.findByEmail(adminEmail);
        if (existing.isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setFullName("Administrator");
            admin.setRole(Role.ADMIN);
            admin.setProvider(Provider.LOCAL);
            admin.setIsActive(true);
            userRepository.save(admin);
        } else if (!existing.get().getIsActive()) {
            // Re-activate the admin if it was deactivated — prevents irreversible lockout
            // on restart.
            existing.get().setIsActive(true);
            userRepository.save(existing.get());
        }
    }
}
