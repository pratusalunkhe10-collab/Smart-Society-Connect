package com.smartsocietyconnect.auth.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a Role in the Smart Society Connect application.
 *
 * <p>Maps to the {@code roles} table in the database. Roles are used to
 * define access levels and permissions within the authentication module.
 *
 * <p>Note: {@code @Data} and {@code @EqualsAndHashCode} are intentionally
 * avoided to prevent issues with JPA lazy-loading proxies and bidirectional
 * relationship cycles.
 *
 * @author  Smart Society Connect Team
 * @version 1.0
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role implements Serializable {

    /**
     * Primary key for the role.
     *
     * <p>Maps to {@code role_id} column (TINYINT UNSIGNED, AUTO_INCREMENT).
     * Using {@link Short} to represent MySQL's TINYINT UNSIGNED (0–255).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Byte  roleId;

    /**
     * Unique name identifying the role (e.g., ADMIN, USER, MODERATOR).
     *
     * <p>Maps to {@code role_name} column (VARCHAR(20), NOT NULL, UNIQUE).
     */
    @Column(name = "role_name", nullable = false, unique = true, length = 20)
    private String roleName;

    /**
     * Human-readable description of the role's purpose and permissions.
     *
     * <p>Maps to {@code role_description} column (VARCHAR(100), nullable).
     */
    @Column(name = "role_description", length = 100)
    private String roleDescription;

}