package com.devannalu.tsworkspace.foundation;

import org.springframework.data.repository.Repository;
import java.util.Optional;

public interface SchemaMarkerRepository extends Repository<SchemaMarker, Integer> {
    Optional<SchemaMarker> findById(Integer id);
}
