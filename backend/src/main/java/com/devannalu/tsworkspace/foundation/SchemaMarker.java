package com.devannalu.tsworkspace.foundation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Technical marker only; no business identity or permission data. */
@Entity
@Table(name = "schema_marker")
public class SchemaMarker {
    @Id
    private Integer id;
    @Column(nullable = false, length = 40)
    private String name;

    protected SchemaMarker() { }

    public String getName() { return name; }
}
