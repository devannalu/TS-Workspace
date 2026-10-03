package com.devannalu.tsworkspace.infraestrutura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "schema_marker")
public class MarcadorInfraestrutura {
    @Id
    private Integer id;
    @Column(nullable = false, length = 40)
    private String name;

    protected MarcadorInfraestrutura() { }

    public String getNome() { return name; }
}
