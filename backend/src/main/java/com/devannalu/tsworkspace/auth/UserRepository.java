package com.devannalu.tsworkspace.auth;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<User, String> {
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    User save(User user);
}
