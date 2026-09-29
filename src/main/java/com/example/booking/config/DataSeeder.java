package com.example.booking.config;

import com.example.booking.entity.Resource;
import com.example.booking.entity.Role;
import com.example.booking.entity.User;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds demo users and resources on startup (disable with SEED_ENABLED=false). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final ResourceRepository resources;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, ResourceRepository resources, PasswordEncoder encoder) {
        this.users = users;
        this.resources = resources;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        seedUser("admin", "admin123", Role.ADMIN);
        seedUser("user1", "user123", Role.USER);
        seedUser("user2", "user123", Role.USER);
        if (resources.count() == 0) {
            resources.save(new Resource("Conference Room A", "10-seat room with projector", "ROOM", true));
            resources.save(new Resource("Toyota Corolla", "Company sedan", "VEHICLE", true));
            resources.save(new Resource("4K Projector", "Portable projector", "EQUIPMENT", true));
            resources.save(new Resource("Boardroom (renovation)", "Temporarily unavailable", "ROOM", false));
        }
        log.info("Seed data ready: admin/admin123, user1/user123, user2/user123");
    }

    private void seedUser(String username, String rawPassword, Role role) {
        if (!users.existsByUsername(username)) {
            users.save(new User(username, encoder.encode(rawPassword), role));
        }
    }
}
