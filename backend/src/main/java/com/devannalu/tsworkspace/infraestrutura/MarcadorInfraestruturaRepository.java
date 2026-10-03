package com.devannalu.tsworkspace.infraestrutura;

import org.springframework.data.repository.Repository;
import java.util.Optional;

public interface MarcadorInfraestruturaRepository extends Repository<MarcadorInfraestrutura, Integer> {
    Optional<MarcadorInfraestrutura> findById(Integer id);
}
