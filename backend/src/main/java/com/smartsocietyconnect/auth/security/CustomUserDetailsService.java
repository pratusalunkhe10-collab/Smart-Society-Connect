package com.smartsocietyconnect.auth.security;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;   // ✅ ADD THIS IMPORT

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.auth.repository.UserRoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom implementation of {@link UserDetailsService} for Spring Security.
 *
 * <p>Loads user credentials and authorities from the database
 * during authentication filter processing.
 *
 * <p>Roles are loaded from the {@code user_roles} join table via
 * {@link UserRoleRepository} because {@link User} entity has no
 * direct roles collection — roles are managed through the
 * {@code UserRole} join entity.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository     userRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * Loads a {@link UserDetails} object by email address.
     *
     * <p>Called automatically by Spring Security during JWT filter
     * validation and form-based authentication.
     *
     * <p>Flow:
     * <ol>
     *   <li>Finds user by email in {@code users} table</li>
     *   <li>Loads role mappings from {@code user_roles} table</li>
     *   <li>Maps each role to a {@link SimpleGrantedAuthority}</li>
     *   <li>Returns Spring Security {@link UserDetails} object</li>
     * </ol>
     *
     * @param email the email address used as the username identifier
     * @return populated {@link UserDetails} with email, password hash, and authorities
     * @throws UsernameNotFoundException if no user exists with the given email
     */
    @Override
    @Transactional(readOnly = true)   // ✅ CRITICAL: Keeps Hibernate session open while loading lazy roles
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        log.debug("Loading user by email: {}", email);

        // 1. Load user from users table
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        // 2. Load roles from user_roles join table
        //    Option A: Simple approach (requires @Transactional on this method)
        List<SimpleGrantedAuthority> authorities = userRoleRepository
                .findByUser(user)
                .stream()
                .map(userRole -> new SimpleGrantedAuthority(
                        userRole.getRole().getRoleName()   // e.g. "ADMIN", "RESIDENT"
                ))
                .collect(Collectors.toList());

        //    Option B (if Option A still fails): Use a JOIN FETCH query in the repository.
        //    Uncomment the lines below and remove Option A.
        /*
        List<UserRole> userRoles = userRoleRepository.findByUserWithRole(user);
        List<SimpleGrantedAuthority> authorities = userRoles.stream()
                .map(userRole -> new SimpleGrantedAuthority(userRole.getRole().getRoleName()))
                .collect(Collectors.toList());
        */

        log.debug("User {} loaded with {} role(s)", email, authorities.size());

        // 3. Return Spring Security UserDetails
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                authorities
        );
    }
}