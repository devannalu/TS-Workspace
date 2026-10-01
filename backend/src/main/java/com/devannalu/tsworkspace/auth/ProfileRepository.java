package com.devannalu.tsworkspace.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

public interface ProfileRepository extends Repository<Profile, String> {
    @EntityGraph(attributePaths = "role")
    Optional<Profile> findById(String userId);
    boolean existsById(String userId);
    Profile save(Profile profile);
}
