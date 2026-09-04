package com.smartsocietyconnect.auth.controller;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

// ✅ FIXED: correct package (dto) and correct class names (Dto suffix)
import com.smartsocietyconnect.auth.dto.AuthResponseDto;
import com.smartsocietyconnect.auth.dto.LoginRequestDto;
import com.smartsocietyconnect.auth.dto.OtpVerificationDto;
import com.smartsocietyconnect.auth.dto.ProfileResponseDto;
import com.smartsocietyconnect.auth.dto.RegisterRequestDto;
import com.smartsocietyconnect.auth.dto.UpdateProfileRequestDto;
import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.exception.AuthException;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.auth.repository.UserRoleRepository;
import com.smartsocietyconnect.auth.service.AuthService;
import com.smartsocietyconnect.resident.service.FileStorageService;
import com.smartsocietyconnect.resident.repository.ResidentRepository;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller exposing authentication endpoints.
 *
 * <p>Base path: {@code /api/auth}
 *
 * <p>Endpoints:
 * <ul>
 *     <li>POST /register    — new user registration + OTP dispatch</li>
 *     <li>POST /login       — credential validation + JWT issuance</li>
 *     <li>POST /verify-otp  — OTP verification + account activation</li>
 *     <li>POST /resend-otp  — resend OTP to registered email</li>
 *     <li>POST /logout      — invalidate JWT session</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Tag(
        name = "Authentication Management",
        description = "APIs for user registration, login, OTP verification, and logout"
)
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final FileStorageService fileStorageService;
    private final ResidentRepository residentRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    // =========================================================================
    // REGISTER
    // =========================================================================

    /**
     * Registers a new user and dispatches OTP to their email.
     *
     * @param request registration DTO (firstName, lastName, email, mobile, password)
     * @return 201 CREATED with user email and full name confirmation
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request  // ✅ FIXED: RegisterRequestDto
    ) {
        log.info("Registration request received for email: {}", request.getEmail());

        // ✅ FIXED: registerUser() — correct method name from AuthService
        AuthResponseDto response = authService.registerUser(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    /**
     * Authenticates user by email or mobile and returns a JWT token.
     *
     * @param request login DTO (username → email or mobile, password)
     * @return 200 OK with JWT token, userId, fullName, roleName
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request  // ✅ FIXED: LoginRequestDto
    ) {
        // ✅ FIXED: getUsername() — LoginRequestDto has 'username' field, NOT 'email'
        log.info("Login request received for username: {}", request.getUsername());

        // ✅ FIXED: loginUser() — correct method name from AuthService
        AuthResponseDto response = authService.loginUser(request);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // VERIFY OTP
    // =========================================================================

    /**
     * Verifies OTP and activates the user account.
     *
     * <p>{@link AuthService#verifyOtp} returns a {@code String} message.
     * Wrapped into {@link AuthResponseDto} so all endpoints return
     * a consistent response structure.
     *
     * @param request OTP verification DTO (email + otpCode)
     * @return 200 OK with success message in AuthResponseDto
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponseDto> verifyOtp(
            @Valid @RequestBody OtpVerificationDto request  // ✅ FIXED: OtpVerificationDto
    ) {
        log.info("OTP verification request received for email: {}", request.getEmail());

        // ✅ FIXED: verifyOtp() returns String — wrap it into AuthResponseDto
        String message = authService.verifyOtp(request);

        AuthResponseDto response = AuthResponseDto.builder()
                .email(request.getEmail())
                // token / userId / roleName intentionally null — account just activated
                .build();

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // RESEND OTP
    // =========================================================================

    /**
     * Resends a fresh OTP to the user's registered email.
     *
     * @param email the registered email address (plain string in request body)
     * @return 200 OK with confirmation
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<AuthResponseDto> resendOtp(
            @RequestBody String email
    ) {
        log.info("Resend OTP request received for email: {}", email);

        // ✅ FIXED: resendOtp() added to AuthService + AuthServiceImpl (see below)
        AuthResponseDto response = authService.resendOtp(email.trim());

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // LOGOUT
    // =========================================================================

    /**
     * Logs out the user by marking the JWT session as logged out
     * in the {@code user_sessions} table.
     *
     * @param token the raw JWT token string (from request body)
     * @return 200 OK with logout confirmation
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponseDto> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) String requestToken
    ) {
        log.info("Logout request received");

        // ✅ FIXED: logout() added to AuthService + AuthServiceImpl (see below)
        AuthResponseDto response = authService.logout(resolveLogoutToken(authorization, requestToken));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponseDto> getProfile(Authentication authentication) {
        return ResponseEntity.ok(toProfile(authentication));
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileResponseDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequestDto request
    ) {
        User user = currentUser(authentication);
        String mobile = request.mobile().trim();

        userRepository.findByMobile(mobile)
                .filter(existing -> !existing.getUserId().equals(user.getUserId()))
                .ifPresent(existing -> { throw new AuthException("Mobile number is already registered"); });

        String[] nameParts = request.name().trim().split("\\s+", 2);
        user.setFirstName(nameParts[0]);
        user.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        user.setMobile(mobile);
        userRepository.save(user);

        return ResponseEntity.ok(toProfile(user));
    }

    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponseDto> uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new AuthException("Profile image must be an image file");
        }

        User user = currentUser(authentication);
        String previousImage = user.getProfileImage();
        user.setProfileImage(fileStorageService.storeFile(file));
        userRepository.save(user);

        if (previousImage != null && !previousImage.isBlank()) {
            fileStorageService.deleteFile(previousImage);
        }

        return ResponseEntity.ok(toProfile(user));
    }

    @GetMapping("/profile/image/{userId}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        if (user.getProfileImage() == null || user.getProfileImage().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path imagePath = uploadRoot.resolve(user.getProfileImage()).normalize();
            if (!imagePath.startsWith(uploadRoot) || !Files.isRegularFile(imagePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource image = new UrlResource(imagePath.toUri());
            String contentType = Files.probeContentType(imagePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
                    .cacheControl(org.springframework.http.CacheControl.noCache())
                    .body(image);
        } catch (MalformedURLException ex) {
            throw new AuthException("Unable to load profile image");
        } catch (java.io.IOException ex) {
            throw new AuthException("Unable to read profile image");
        }
    }

    private ProfileResponseDto toProfile(Authentication authentication) {
        return toProfile(currentUser(authentication));
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AuthException("Authenticated user not found"));
    }

    private ProfileResponseDto toProfile(User user) {
        String flat = residentRepository.findByUserUserId(user.getUserId())
                .filter(resident -> resident.getMoveOutDate() == null && resident.getFlat() != null)
                .map(resident -> {
                    String wing = resident.getFlat().getWing();
                    String number = resident.getFlat().getFlatNumber();
                    return wing == null || wing.isBlank() || number == null || number.startsWith(wing)
                            ? number : wing + "-" + number;
                })
                .orElse(null);
        List<String> priority = List.of("ADMIN", "SECRETARY", "ACCOUNTANT", "SECURITY", "RESIDENT");
        List<String> roles = userRoleRepository.findByUser(user).stream()
                .map(mapping -> mapping.getRole().getRoleName()).toList();
        String activeRole = priority.stream().filter(roles::contains).findFirst().orElse("RESIDENT");
        return ProfileResponseDto.builder()
                .id(user.getUserId())
                .name((user.getFirstName() + " " + (user.getLastName() == null ? "" : user.getLastName())).trim())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .flat(flat)
                .roleName(activeRole)
                .profileImageUrl(user.getProfileImage() == null || user.getProfileImage().isBlank()
                        ? null
                        : "/api/auth/profile/image/" + user.getUserId())
                .build();
    }

    private String resolveLogoutToken(String authorization, String requestToken) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }

        if (requestToken == null || requestToken.isBlank()) {
            throw new AuthException("A bearer token is required to log out");
        }

        String token = requestToken.trim();
        return token.startsWith("\"") && token.endsWith("\"")
                ? token.substring(1, token.length() - 1)
                : token;
    }
}
