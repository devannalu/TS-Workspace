package com.devannalu.tsworkspace.usuarios;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface UsuarioRepository extends Repository<Usuario, String> {
    Optional<Usuario> findById(String id);
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    Usuario save(Usuario user);
}
