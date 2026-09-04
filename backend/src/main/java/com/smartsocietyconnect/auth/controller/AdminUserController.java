package com.smartsocietyconnect.auth.controller;

//import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartsocietyconnect.auth.dto.AdminUserApprovalRequest;
import com.smartsocietyconnect.auth.dto.AdminUserResponseDto;
import com.smartsocietyconnect.auth.entity.Role;
import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.entity.UserRole;
import com.smartsocietyconnect.auth.exception.AuthException;
import com.smartsocietyconnect.auth.repository.RoleRepository;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.auth.repository.UserRoleRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Admin-only account approval, activation, and multi-role assignment. */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {
    private static final List<String> ALLOWED_ROLES = List.of("ADMIN", "RESIDENT", "SECURITY", "SECRETARY", "ACCOUNTANT");
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @GetMapping
    public ResponseEntity<List<AdminUserResponseDto>> list(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(user -> status == null || status.isBlank() || status.equalsIgnoreCase(user.getApprovalStatus()))
                .map(this::toResponse)
                .toList());
    }

    @PutMapping("/{userId}/approve")
    @Transactional
    public ResponseEntity<AdminUserResponseDto> approve(@PathVariable Integer userId,
            @Valid @RequestBody AdminUserApprovalRequest request) {
        User user = getUser(userId);
        if (!Boolean.TRUE.equals(user.getIsVerified())) throw new AuthException("User must verify their email before approval");
        // Pending applicants have no role. Replace any legacy role left by the
        // former registration flow with the administrator's selected role.
        userRoleRepository.deleteByUser(user);
        userRoleRepository.flush();
        addRole(user, request.role());
        user.setApprovalStatus("APPROVED");
        user.setIsActive(true);
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    @PutMapping("/{userId}/reject")
    @Transactional
    public ResponseEntity<AdminUserResponseDto> reject(@PathVariable Integer userId) {
        User user = getUser(userId);
        userRoleRepository.deleteByUser(user);
        user.setApprovalStatus("REJECTED");
        user.setIsActive(false);
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    @PutMapping("/{userId}/role")
    @Transactional
    public ResponseEntity<AdminUserResponseDto> changeRole(@PathVariable Integer userId,
            @Valid @RequestBody AdminUserApprovalRequest request) {
        User user = getUser(userId);
        if (!"APPROVED".equalsIgnoreCase(user.getApprovalStatus())) {
            throw new AuthException("Approve the account before assigning an additional role");
        }
        addRole(user, request.role());
        return ResponseEntity.ok(toResponse(user));
    }

    @DeleteMapping("/{userId}/role/{roleName}")
    @Transactional
    public ResponseEntity<AdminUserResponseDto> removeRole(@PathVariable Integer userId,
            @PathVariable String roleName) {
        User user = getUser(userId);
        Role role = getRole(roleName);
        if (!userRoleRepository.existsByUserAndRole(user, role)) {
            throw new AuthException("This role is not assigned to the user");
        }
        if (userRoleRepository.findByUser(user).size() <= 1) {
            throw new AuthException("A user must retain at least one role");
        }
        userRoleRepository.deleteByUserAndRole(user, role);
        return ResponseEntity.ok(toResponse(user));
    }

    private User getUser(Integer id) { return userRepository.findById(id).orElseThrow(() -> new AuthException("User not found")); }
    private void addRole(User user, String rawRole) {
        Role role = getRole(rawRole);
        if (!userRoleRepository.existsByUserAndRole(user, role)) {
            userRoleRepository.save(UserRole.builder().user(user).role(role).build());
        }
    }
    private Role getRole(String rawRole) {
        String roleName = rawRole.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(roleName)) throw new AuthException("Unsupported role");
        return roleRepository.findByRoleName(roleName).orElseThrow(() -> new AuthException("Role not found: " + roleName));
    }
    private AdminUserResponseDto toResponse(User user) {
        List<String> roles = userRoleRepository.findByUser(user).stream().map(mapping -> mapping.getRole().getRoleName()).toList();
        return new AdminUserResponseDto(user.getUserId(), (user.getFirstName() + " " + (user.getLastName() == null ? "" : user.getLastName())).trim(),
                user.getEmail(), user.getMobile(), Boolean.TRUE.equals(user.getIsVerified()), Boolean.TRUE.equals(user.getIsActive()),
                user.getApprovalStatus(), roles, user.getCreatedAt());
    }
}
