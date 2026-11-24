package com.petpal.identity;

import com.petpal.common.config.PetPalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PetPalProperties properties;

    AdminAccountInitializer(UserRepository users, PasswordEncoder passwordEncoder, PetPalProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = AuthService.normaliseEmail(properties.admin().email());
        if (users.existsByEmail(email)) {
            return;
        }
        users.save(User.register(email, passwordEncoder.encode(properties.admin().password()),
                "PetPal", "Admin", null, "Head office", Role.ADMIN));
        log.info("Created admin account {}", email);
    }
}
