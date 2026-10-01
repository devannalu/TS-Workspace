package com.devannalu.tsworkspace.rbac;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface RoleRepository extends Repository<Role, String> {
    Optional<Role> findByKey(String key);
    List<Role> findAllByOrderByKeyAsc();
}
