package com.smartsocietyconnect.auth.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key class for {@link UserRole}.
 *
 * <p>This class represents the composite key consisting of:
 * <ul>
 *     <li>user_id</li>
 *     <li>role_id</li>
 * </ul>
 *
 * <p>Required by JPA when using {@code @IdClass}.
 *
 * <p>Important:
 * <ul>
 *     <li>Must implement {@link Serializable}</li>
 *     <li>Must override {@code equals()} and {@code hashCode()}</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
public class UserRoleId implements Serializable {

    /**
     * User ID part of composite key.
     */
    private Integer user;

    /**
     * Role ID part of composite key.
     */
    private Byte  role;

    /**
     * Default constructor.
     */
    public UserRoleId() {
    }

    /**
     * Parameterized constructor.
     *
     * @param user user ID
     * @param role role ID
     */
    public UserRoleId(Integer user, Byte role) {
        this.user = user;
        this.role = role;
    }

    public Integer getUser() {
        return user;
    }

    public void setUser(Integer user) {
        this.user = user;
    }

    public Byte getRole() {
        return role;
    }

    public void setRole(Byte role) {
        this.role = role;
    }

    /**
     * Compares composite keys.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof UserRoleId that)) return false;

        return Objects.equals(user, that.user)
                && Objects.equals(role, that.role);
    }

    /**
     * Generates hashcode for composite key.
     */
    @Override
    public int hashCode() {
        return Objects.hash(user, role);
    }
}