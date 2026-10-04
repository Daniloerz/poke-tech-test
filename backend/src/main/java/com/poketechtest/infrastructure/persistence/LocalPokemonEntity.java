package com.poketechtest.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "local_pokemon")
@Getter
@Setter
@NoArgsConstructor
public class LocalPokemonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poke_api_id", nullable = false)
    private int pokeApiId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sprite_url", length = 500)
    private String spriteUrl;

    // PostgreSQL text[]: the lists are stored in the row.
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "types", nullable = false)
    private List<String> types = new ArrayList<>();

    @Column(name = "height_decimetres", nullable = false)
    private int heightDecimetres;

    @Column(name = "weight_hectograms", nullable = false)
    private int weightHectograms;

    @Column(name = "localized_name", length = 100)
    private String localizedName;

    @Column(name = "region", length = 100)
    private String region;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", nullable = false)
    private List<String> tags = new ArrayList<>();

    // Filled by the database default (now()) and read back by Hibernate after the insert.
    @Generated
    @Column(name = "synced_at", nullable = false, insertable = false, updatable = false)
    private Instant syncedAt;

    // Set by Hibernate on insert and on every update, from the database clock like synced_at.
    @UpdateTimestamp(source = SourceType.DB)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
