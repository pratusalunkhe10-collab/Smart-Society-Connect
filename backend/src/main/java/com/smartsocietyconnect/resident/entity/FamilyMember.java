package com.smartsocietyconnect.resident.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a family member of a resident in Smart Society Connect.
 *
 * <p>Maps to the {@code family_members} table in the database.
 *
 * <p>Each family member is associated with exactly one {@link Resident}.
 * This allows a flat's occupant to register dependants and co-residents
 * such as parents, spouse, and children for society records.
 *
 * <p><b>DB-level cascade:</b> The {@code family_members} table declares
 * {@code ON DELETE CASCADE} on the {@code resident_id} foreign key. When a
 * {@link Resident} is deleted, all associated {@code FamilyMember} records
 * are automatically removed by the database — no JPA cascade configuration
 * is needed or applied on this entity.
 *
 * <p><b>Lombok constructor strategy:</b>
 * <ul>
 *     <li>{@code @NoArgsConstructor} — required by JPA specification</li>
 *     <li>{@code @AllArgsConstructor(access = AccessLevel.PRIVATE)} — required
 *         by {@code @Builder} internally for its {@code build()} method.
 *         {@code PRIVATE} prevents external code from bypassing the builder
 *         while keeping the constructor visible to Lombok and the IDE.</li>
 *     <li>{@code @Builder} — the sole public construction API</li>
 * </ul>
 *
 * <p>Note: {@code @Data} and {@code @EqualsAndHashCode} are intentionally
 * avoided to prevent issues with JPA lazy-loading proxies and
 * bidirectional relationship cycles.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Entity
@Table(
    name = "family_members",
    indexes = {
        /**
         * Index on resident_id FK for fast family member lookups per resident.
         * Without this, every "find family by resident" query causes a full table scan.
         */
        @Index(name = "idx_family_resident_id", columnList = "resident_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FamilyMember {

    /**
     * Primary key for the family member record.
     *
     * <p>Maps to {@code member_id} column (INT UNSIGNED, AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Integer memberId;

    /**
     * The resident this family member belongs to.
     *
     * <p>Maps to {@code resident_id} foreign key column (INT UNSIGNED, NOT NULL).
     *
     * <p>{@code FetchType.LAZY} defers loading of the {@link Resident} entity
     * until explicitly accessed, avoiding unnecessary joins on family member queries.
     *
     * <p>{@code optional = false} aligns the ORM-level constraint with the
     * DB-level {@code NOT NULL}, enabling Hibernate to use {@code INNER JOIN}
     * instead of {@code LEFT JOIN} when loading the resident.
     *
     * <p><b>Cascade note:</b> The DB declares {@code ON DELETE CASCADE} on this
     * FK — deleting the parent {@link Resident} automatically removes all
     * associated {@code FamilyMember} rows at the database level. No JPA
     * {@code cascade} is configured here as it is not needed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    /**
     * Full name of the family member.
     *
     * <p>Maps to {@code member_name} column (VARCHAR(100), NOT NULL).
     */
    @Column(name = "member_name", nullable = false, length = 100)
    private String memberName;

    /**
     * Relationship of this family member to the primary resident.
     *
     * <p>Maps to {@code relation} column
     * ({@code ENUM('FATHER','MOTHER','SPOUSE','SON','DAUGHTER','BROTHER','SISTER','OTHER')},
     * NOT NULL).
     *
     * <p>Uses {@code EnumType.STRING} — Java constant names match DB ENUM values exactly,
     * so no {@link jakarta.persistence.AttributeConverter} is required.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "relation",
            nullable = false,
            columnDefinition =
                    "ENUM('FATHER','MOTHER','SPOUSE','SON','DAUGHTER','BROTHER','SISTER','OTHER')"
    )
    private RelationType relation;

    /**
     * Age of the family member in years.
     *
     * <p>Maps to {@code age} column (TINYINT UNSIGNED, nullable).
     * {@code Short} is the correct Java type for TINYINT UNSIGNED (0–255);
     * Java's {@code Byte} is signed (-128 to 127) and cannot safely hold
     * values above 127.
     */
    @Column(name = "age")
    private Byte  age;

    /**
     * Mobile number of the family member.
     *
     * <p>Maps to {@code mobile} column (CHAR(10), nullable).
     * Fixed-length {@code CHAR(10)} is used since mobile numbers are
     * always exactly 10 digits.
     */
    @Column(name = "mobile", columnDefinition = "CHAR(10)")
    private String mobile;

    /**
     * Timestamp when this family member record was created.
     *
     * <p>Maps to {@code created_at} column (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP).
     * Set automatically by Hibernate before INSERT via {@code @CreationTimestamp},
     * making the value immediately available on the Java object after
     * {@code save()} — without needing to re-fetch the entity from the DB.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}