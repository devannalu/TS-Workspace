package com.devannalu.tsworkspace.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;

    public BootstrapService(UserRepository users, ProfileRepository profiles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createFirstIdentity(String name, String email, String password) {
        if (name == null || name.isBlank() || password == null || password.length() < 12) {
            throw new IllegalArgumentException("Dados de bootstrap inválidos.");
        }
        String normalizedEmail = EmailNormalizer.normalize(email);
        User existing = users.findByEmail(normalizedEmail).orElse(null);
        if (existing != null) {
            if (!profiles.existsById(existing.getId())) {
                throw new IllegalStateException("Estado parcial de bootstrap detectado.");
            }
            return;
        }
        if (users.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Estado parcial de bootstrap detectado.");
        }
        User user = users.save(new User(name.trim(), normalizedEmail, passwordEncoder.encode(password)));
        profiles.save(new Profile(user.getId(), ProfileStatus.ACTIVE));
    }
}
