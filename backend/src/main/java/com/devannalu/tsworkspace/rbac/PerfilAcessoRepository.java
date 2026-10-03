package com.devannalu.tsworkspace.rbac;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface PerfilAcessoRepository extends Repository<PerfilAcesso, String> {
    Optional<PerfilAcesso> findByKey(String key);
    List<PerfilAcesso> findAllByOrderByKeyAsc();
}
