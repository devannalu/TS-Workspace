package com.devannalu.tsworkspace.autenticacao;

import com.devannalu.tsworkspace.auth.EmailNormalizer;
import com.devannalu.tsworkspace.auth.Profile;
import com.devannalu.tsworkspace.auth.ProfileRepository;
import com.devannalu.tsworkspace.auth.ProfileStatus;
import com.devannalu.tsworkspace.auth.User;
import com.devannalu.tsworkspace.auth.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.devannalu.tsworkspace.rbac.PerfilAcessoRepository;
import com.devannalu.tsworkspace.rbac.RbacSeed;

@Service
public class InicializacaoIdentidadeService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final PerfilAcessoRepository roles;
    private final RbacSeed seed;

    public InicializacaoIdentidadeService(UserRepository users, ProfileRepository profiles, PasswordEncoder passwordEncoder, PerfilAcessoRepository roles, RbacSeed seed) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.roles = roles;
        this.seed = seed;
    }

    @Transactional
    public void criarPrimeiraIdentidade(String name, String email, String password) {
        if (name == null || name.isBlank() || password == null || password.length() < 12) {
            throw new IllegalArgumentException("Dados de bootstrap inválidos.");
        }
        String normalizedEmail = EmailNormalizer.normalize(email);
        seed.seed();
        var superAdmin = roles.findByKey("SUPER_ADMIN").orElseThrow();
        User existing = users.findByEmail(normalizedEmail).orElse(null);
        if (existing != null) {
            Profile profile = profiles.findById(existing.getId()).orElse(null);
            if (profile == null || profile.getStatus() != ProfileStatus.ACTIVE || profile.getRole() == null
                || !profile.getRole().getKey().equals("SUPER_ADMIN")) {
                throw new IllegalStateException("Estado parcial de bootstrap detectado.");
            }
            return;
        }
        if (users.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Estado parcial de bootstrap detectado.");
        }
        User user = users.save(new User(name.trim(), normalizedEmail, passwordEncoder.encode(password)));
        profiles.save(new Profile(user.getId(), ProfileStatus.ACTIVE, superAdmin));
    }
}
