package com.devannalu.tsworkspace.rbac;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface PermissaoRepository extends Repository<Permissao, String> {
    Optional<Permissao> findByKey(String key);
    List<Permissao> findAllByOrderByKeyAsc();
}
