package com.devannalu.tsworkspace.rbac;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface PermissionRepository extends Repository<Permission, String> {
    Optional<Permission> findByKey(String key);
    List<Permission> findAllByOrderByKeyAsc();
}
