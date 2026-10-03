package com.devannalu.tsworkspace.autenticacao;

import com.devannalu.tsworkspace.auth.EmailNormalizer;
import com.devannalu.tsworkspace.usuarios.Perfil;
import com.devannalu.tsworkspace.usuarios.PerfilRepository;
import com.devannalu.tsworkspace.auth.ProfileStatus;
import com.devannalu.tsworkspace.usuarios.Usuario;
import com.devannalu.tsworkspace.usuarios.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.devannalu.tsworkspace.rbac.PerfilAcessoRepository;
import com.devannalu.tsworkspace.rbac.RbacSeed;

@Service
public class InicializacaoIdentidadeService {
    private final UsuarioRepository usuarios;
    private final PerfilRepository perfis;
    private final PasswordEncoder passwordEncoder;
    private final PerfilAcessoRepository roles;
    private final RbacSeed seed;

    public InicializacaoIdentidadeService(UsuarioRepository usuarios, PerfilRepository perfis, PasswordEncoder passwordEncoder, PerfilAcessoRepository roles, RbacSeed seed) {
        this.usuarios = usuarios;
        this.perfis = perfis;
        this.passwordEncoder = passwordEncoder;
        this.roles = roles;
        this.seed = seed;
    }

    @Transactional
    public void criarPrimeiraIdentidade(String name, String email, String password) {
        if (name == null || name.isBlank() || password == null || password.length() < 12) {
            throw new IllegalArgumentException("Dados de bootstrap inválidos.");
        }
        String emailNormalizado = EmailNormalizer.normalize(email);
        seed.seed();
        var superAdmin = roles.findByKey("SUPER_ADMIN").orElseThrow();
        Usuario usuarioExistente = usuarios.findByEmail(emailNormalizado).orElse(null);
        if (usuarioExistente != null) {
            Perfil perfil = perfis.findById(usuarioExistente.getId()).orElse(null);
            if (perfil == null || perfil.getStatus() != ProfileStatus.ACTIVE || perfil.getPerfilAcesso() == null
                || !perfil.getPerfilAcesso().getKey().equals("SUPER_ADMIN")) {
                throw new IllegalStateException("Estado parcial de bootstrap detectado.");
            }
            return;
        }
        if (usuarios.existsByEmail(emailNormalizado)) {
            throw new IllegalStateException("Estado parcial de bootstrap detectado.");
        }
        Usuario usuario = usuarios.save(new Usuario(name.trim(), emailNormalizado, passwordEncoder.encode(password)));
        perfis.save(new Perfil(usuario.getId(), ProfileStatus.ACTIVE, superAdmin));
    }
}
