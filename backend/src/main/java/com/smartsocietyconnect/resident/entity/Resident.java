package com.smartsocietyconnect.resident.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.smartsocietyconnect.auth.entity.User;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a resident in the Smart Society Connect application.
 *
 * <p>Maps to the {@code residents} table in the database.
 *
 * <p>A resident links a {@link User} account to a {@link Flat}, along with
 * details such as residency type, emergency contact, and move-in/move-out dates.
 * Family members are linked to a resident via the {@code family_members} table.
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
    name = "residents",
    indexes = {
        @Index(name = "idx_resident_flat_id", columnList = "flat_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Resident {

    /**
     * Primary key for the resident record.
     *
     * <p>Maps to {@code resident_id} column (INT UNSIGNED, AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resident_id")
    private Integer residentId;

    /**
     * The user account associated with this resident.
     *
     * <p>Maps to {@code user_id} foreign key column (INT UNSIGNED, NOT NULL, UNIQUE).
     * The {@code UNIQUE} constraint on the FK ensures one-to-one mapping —
     * each user can be a resident in at most one flat.
     *
     * <p>{@code FetchType.LAZY} defers loading of the {@link User} entity
     * until explicitly accessed, avoiding unnecessary joins on resident queries.
     *
     * <p>{@code optional = false} aligns the ORM-level constraint with the
     * DB-level {@code NOT NULL}, enabling Hibernate to use {@code INNER JOIN}
     * instead of {@code LEFT JOIN} when loading the user.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * The flat occupied by this resident.
     *
     * <p>Maps to {@code flat_id} foreign key column (SMALLINT UNSIGNED, NOT NULL).
     * Multiple residents may be linked to the same flat (e.g. owner + family tenants).
     *
     * <p>{@code FetchType.LAZY} defers loading of the {@link Flat} entity
     * until explicitly accessed.
     *
     * <p>{@code optional = false} aligns the ORM-level constraint with the
     * DB-level {@code NOT NULL}, enabling Hibernate to use {@code INNER JOIN}.
     *
     * <p>No {@code cascade} is applied — resident records do not manage
     * the lifecycle of flats.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;

    /**
     * Whether the resident is an owner or tenant.
     *
     * <p>Maps to {@code resident_type} column
     * ({@code ENUM('OWNER','TENANT')}, NOT NULL).
     * Uses {@code EnumType.STRING} — Java constant names match DB ENUM values exactly.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resident_type", nullable = false,
            columnDefinition = "ENUM('OWNER','TENANT')")
    private ResidentType residentType;

    /**
     * The resident's occupation or profession.
     *
     * <p>Maps to {@code occupation} column (VARCHAR(100), nullable).
     */
    @Column(name = "occupation", length = 100)
    private String occupation;

    /**
     * Emergency contact mobile number for this resident.
     *
     * <p>Maps to {@code emergency_contact} column (CHAR(10), nullable).
     * Fixed-length {@code CHAR(10)} is used since mobile numbers are
     * always exactly 10 digits.
     */
    @Column(name = "emergency_contact", columnDefinition = "CHAR(10)")
    private String emergencyContact;

    /**
     * Date the resident moved into the flat.
     *
     * <p>Maps to {@code move_in_date} column (DATE, NOT NULL).
     */
    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    /**
     * Date the resident moved out of the flat.
     *
     * <p>Maps to {@code move_out_date} column (DATE, nullable).
     * {@code null} indicates the resident is currently active in the flat.
     */
    @Column(name = "move_out_date")
    private LocalDate moveOutDate;

    /**
     * Indicates whether this resident is the primary member of the flat.
     *
     * <p>Maps to {@code is_primary_member} column (TINYINT(1), NOT NULL, DEFAULT 1).
     *
     * <p>{@code @Builder.Default} ensures this value is honoured when constructing
     * via the Lombok builder; without it, {@code @Builder} silently sets the
     * field to {@code null}.
     *
     * <p>{@code nullable = false} enforces the NOT NULL constraint at the JPA
     * DDL level. The {@code columnDefinition} alone is not sufficient for
     * JPA schema generation.
     */
    @Builder.Default
    @Column(name = "is_primary_member", nullable = false,
            columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean isPrimaryMember = true;

    /**
     * Timestamp when this resident record was created.
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