package com.elms.config;

import com.elms.entity.Role;
import com.elms.entity.User;
import com.elms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile({"dev", "default"})
public class DevelopmentDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevelopmentDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String email = "hr@example.com";

        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        User hrUser = new User();
        hrUser.setEmail(email);
        hrUser.setPassword(passwordEncoder.encode("test123"));
        hrUser.setRole(Role.HR);
        hrUser.setEnabled(true);
        LocalDateTime now = LocalDateTime.now();
        hrUser.setCreatedAt(now);
        hrUser.setUpdatedAt(now);

        userRepository.save(hrUser);
    }
}
