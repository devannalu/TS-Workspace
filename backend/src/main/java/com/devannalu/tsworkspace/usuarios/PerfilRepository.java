package com.devannalu.tsworkspace.usuarios;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

public interface PerfilRepository extends Repository<Perfil, String> {
    @EntityGraph(attributePaths = "perfilAcesso")
    Optional<Perfil> findById(String userId);
    boolean existsById(String userId);
    Perfil save(Perfil profile);
}
