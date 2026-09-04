package com.smartsocietyconnect.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing the mapping between users and roles.
 *
 * <p>Maps to the {@code user_roles} table in the database.
 *
 * <p>This entity implements a many-to-many relationship between
 * {@link User} and {@link Role}.
 *
 * <p>A single user can have multiple roles, and a single role
 * can belong to multiple users.
 *
 * <p>Composite primary key is implemented using {@code @IdClass}.
 *
 * <p>Note: {@code FetchType.LAZY} is used to avoid unnecessary
 * loading of related entities.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Entity
@Table(name = "user_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserRoleId.class)
public class UserRole {

    /**
     * User associated with the role mapping.
     *
     * <p>Maps to {@code user_id} foreign key column.
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Role associated with the user mapping.
     *
     * <p>Maps to {@code role_id} foreign key column.
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

}