package com.smartsocietyconnect.auth.service.impl;

import com.smartsocietyconnect.auth.dto.AuthResponseDto;
import com.smartsocietyconnect.auth.dto.LoginRequestDto;
import com.smartsocietyconnect.auth.dto.OtpVerificationDto;
import com.smartsocietyconnect.auth.dto.RegisterRequestDto;
import com.smartsocietyconnect.auth.entity.OtpVerification;
import com.smartsocietyconnect.auth.entity.Role;
import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.entity.UserRole;
import com.smartsocietyconnect.auth.entity.UserSession;
import com.smartsocietyconnect.auth.exception.AuthException;
import com.smartsocietyconnect.auth.repository.OtpVerificationRepository;
import com.smartsocietyconnect.auth.repository.RoleRepository;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.auth.repository.UserRoleRepository;
import com.smartsocietyconnect.auth.repository.UserSessionRepository;
import com.smartsocietyconnect.auth.service.AuthService;
import com.smartsocietyconnect.auth.service.EmailService;
import com.smartsocietyconnect.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Implementation of {@link AuthService}.
 *
 * <p>Handles user registration, login, and OTP verification aligned
 * exactly with the entity, DTO, and repository layer as defined.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /**
     * A user may have more than one role. The first matching role is the
     * active workspace returned to the frontend at login; Spring Security
     * still loads every assigned role for authorization.
     */
    private static final List<String> ROLE_PRIORITY = List.of(
            "ADMIN", "SECRETARY", "ACCOUNTANT", "SECURITY", "RESIDENT"
    );

    private static final int OTP_EXPIRY_MINUTES = 5;

    private final UserRepository            userRepository;
    private final RoleRepository            roleRepository;
    private final UserRoleRepository        userRoleRepository;
    private final OtpVerificationRepository otpRepository;
    private final UserSessionRepository     sessionRepository;
    private final PasswordEncoder           passwordEncoder;
    private final JwtUtil                   jwtUtil;
    private final EmailService              emailService;
    
 // =========================================================================
 // RESEND OTP
 // =========================================================================

 /**
  * Generates and sends a fresh OTP for an existing unverified user.
  *
  * <p>Flow:
  * <ol>
  *   <li>Looks up user by email</li>
  *   <li>Checks account is not already verified</li>
  *   <li>Generates new OTP and saves to otp_verification</li>
  *   <li>Sends OTP email</li>
  * </ol>
  *
  * @param email registered email address
  * @return confirmation AuthResponseDto
  */
 @Override
 @Transactional
 public AuthResponseDto resendOtp(String email) {
     log.info("Resend OTP requested for: {}", email);

     // 1. User lookup
     User user = userRepository.findByEmail(email)
             .orElseThrow(() -> new AuthException("User not found: " + email));

     // 2. Already verified check
     if (Boolean.TRUE.equals(user.getIsVerified()))
         throw new AuthException("Account is already verified. Please log in.");

     // 3. Generate new OTP and persist
     String otpCode = generateOtp();
     OtpVerification otp = OtpVerification.builder()
             .user(user)
             .otpCode(otpCode)
             .expiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
             .build();

     otpRepository.save(otp);

     // 4. Send OTP email
     emailService.sendOtp(user.getEmail(), otpCode);
     log.info("OTP resent to: {}", email);

     return AuthResponseDto.builder()
             .email(user.getEmail())
             .fullName(user.getFirstName() + " " + user.getLastName())
             .build();
 }

 // =========================================================================
 // LOGOUT
 // =========================================================================

 /**
  * Logs out a user by marking their JWT session as logged out
  * in the {@code user_sessions} table.
  *
  * <p>Flow:
  * <ol>
  *   <li>Finds active session by JWT token</li>
  *   <li>Sets {@code isLoggedOut = true} and records {@code logoutTime}</li>
  * </ol>
  *
  * @param token raw JWT token string
  * @return confirmation AuthResponseDto
  */
 @Override
 @Transactional
 public AuthResponseDto logout(String token) {
     log.info("Logout requested");

     // 1. Find session by JWT token
     UserSession session = sessionRepository.findByJwtToken(token)
             .orElseThrow(() -> new AuthException("Session not found for provided token."));

     // 2. Mark session as logged out
     session.setIsLoggedOut(true);
     session.setLogoutTime(LocalDateTime.now());
     sessionRepository.save(session);

     log.info("User logged out, sessionId={}", session.getSessionId());

     return AuthResponseDto.builder()
             .email(session.getUser().getEmail())
             .build();
 }

    // =========================================================================
    // REGISTER
    // =========================================================================

    /**
     * Registers a new user.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validates no duplicate email / mobile</li>
     *   <li>Persists {@link User} with {@code isVerified = false}</li>
     *   <li>Bootstraps the first account as ADMIN; later applicants remain unassigned</li>
     *   <li>Generates 6-digit OTP and stores in {@code otp_verification}</li>
     *   <li>Dispatches OTP email — no JWT issued yet</li>
     * </ol>
     *
     * @param request registration DTO
     * @return {@link AuthResponseDto} with confirmation message (no token)
     */
    @Override
    @Transactional
    public AuthResponseDto registerUser(RegisterRequestDto request) {
        log.info("Registering user: {}", request.getEmail());

        // A fresh installation has no one who can approve registrations.
        // Bootstrap the first verified account as the system administrator.
        boolean firstSystemUser = userRepository.count() == 0;

        // 1. Duplicate guards
        if (userRepository.existsByEmail(request.getEmail()))
            throw new AuthException("Email already registered: " + request.getEmail());

        // Normalize mobile → always store exactly 10 digits (strip +91 / 91 prefix)
        String mobile = normalizeMobile(request.getMobile());

        if (userRepository.existsByMobile(mobile))
            throw new AuthException("Mobile already registered: " + mobile);

        // 2. Persist user — isVerified stays FALSE until OTP confirmed
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .mobile(mobile)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                // The first account bootstraps the society; all later accounts
                // remain pending until an administrator approves them.
                .isActive(firstSystemUser)
                .approvalStatus(firstSystemUser ? "APPROVED" : "PENDING")
                .build();

        userRepository.save(user);
        log.debug("User persisted, id={}", user.getUserId());

        // 3. Bootstrap only the first account as ADMIN. Every later applicant
        //    receives their initial role when an administrator approves them.
        if (firstSystemUser) {
            Role role = roleRepository.findByRoleName("ADMIN")
                    .orElseThrow(() -> new AuthException("Default role ADMIN not found in DB"));
            if (!userRoleRepository.existsByUserAndRole(user, role)) {
                userRoleRepository.save(UserRole.builder().user(user).role(role).build());
            }
        }

        // 4. Generate OTP and persist in otp_verification
        String otpCode = generateOtp();
        OtpVerification otp = OtpVerification.builder()
                .user(user)
                .otpCode(otpCode)
                .expiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                // isUsed uses @Builder.Default → false automatically
                .build();

        otpRepository.save(otp);

        // 5. Deliver OTP
        emailService.sendOtp(user.getEmail(), otpCode);
        log.info("OTP dispatched to {}", user.getEmail());

        return AuthResponseDto.builder()
                .email(user.getEmail())
                .fullName(user.getFirstName() + " " + user.getLastName())
                // AuthResponseDto has no 'message' field — use fullName + email as confirmation
                // token and roleName intentionally null at registration stage
                .build();
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    /**
     * Authenticates a user by email or mobile number.
     *
     * <p>Flow:
     * <ol>
     *   <li>Resolves user by {@code username} (email or 10-digit mobile)</li>
     *   <li>Verifies BCrypt password</li>
     *   <li>Checks {@code isActive} and {@code isVerified} flags</li>
     *   <li>Loads primary role from {@code user_roles} table</li>
     *   <li>Issues JWT via {@link JwtUtil}</li>
     *   <li>Persists session in {@code user_sessions}</li>
     * </ol>
     *
     * <p>Note: {@code LoginRequestDto.username} accepts email or mobile.
     * IP and device info are not in the DTO — they must be injected from
     * the HTTP layer (controller) and passed separately, or added to the DTO.
     *
     * @param request login DTO (username + password)
     * @return {@link AuthResponseDto} with JWT, role, and user info
     */
    @Override
    @Transactional
    public AuthResponseDto loginUser(LoginRequestDto request) {
        log.info("Login attempt for username: {}", request.getUsername());

        // 1. Resolve user — username is email or mobile
        User user = resolveUserByUsername(request.getUsername());

        // 2. Password verification
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new AuthException("Invalid credentials");

        // 3. Account status checks
        //    Boolean (not boolean) — use Boolean.TRUE.equals() to avoid NPE
        if (!Boolean.TRUE.equals(user.getIsVerified()))
            throw new AuthException("Account not verified. Please verify OTP first.");

        if (!"APPROVED".equals(user.getApprovalStatus()))
            throw new AuthException("Your account is pending admin approval.");

        if (!Boolean.TRUE.equals(user.getIsActive()))
            throw new AuthException("Account is disabled. Contact admin.");

        // 4. Load every role and select the active frontend role predictably.
        // Repository order is not guaranteed, so findFirst() could return
        // RESIDENT for a user who is also a SECRETARY.
        List<String> assignedRoles = userRoleRepository.findByUser(user).stream()
                .map(ur -> ur.getRole().getRoleName())
                .toList();
        String primaryRole = ROLE_PRIORITY.stream()
                .filter(assignedRoles::contains)
                .findFirst()
                .orElseThrow(() -> new AuthException("No system role is assigned to this account. Contact admin."));

        // 5. Issue JWT
        String token = jwtUtil.generateToken(user.getEmail(), primaryRole);

        // 6. Persist session — ipAddress and deviceInfo not in LoginRequestDto;
        //    pass null here — controller layer should enrich if needed
        UserSession session = UserSession.builder()
                .user(user)
                .jwtToken(token)
                .ipAddress(null)    // enrich from HttpServletRequest in controller
                .deviceInfo(null)   // enrich from User-Agent header in controller
                // isLoggedOut uses @Builder.Default → false automatically
                .build();

        sessionRepository.save(session);
        log.info("Session created for userId={}", user.getUserId());

        return AuthResponseDto.builder()
                .token(token)
                .email(user.getEmail())
                .userId(user.getUserId())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .roleName(primaryRole)
                .profileImageUrl(user.getProfileImage() == null || user.getProfileImage().isBlank()
                        ? null
                        : "/api/auth/profile/image/" + user.getUserId())
                .build();
    }

    // =========================================================================
    // VERIFY OTP
    // =========================================================================

    /**
     * Verifies a 6-digit OTP and activates the user account.
     *
     * <p>Flow:
     * <ol>
     *   <li>Resolves user by email</li>
     *   <li>Fetches latest unused, non-expired OTP atomically from DB</li>
     *   <li>Validates the supplied OTP code</li>
     *   <li>Marks OTP as {@code is_used = true}</li>
     *   <li>Sets {@code is_verified = true} on the user</li>
     * </ol>
     *
     * <p>Expiry is enforced at the DB query level via
     * {@code findTopByUserAndIsUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc},
     * eliminating a race-condition window that would exist if checked in Java.
     *
     * @param request OTP verification DTO (email + otpCode)
     * @return success message string
     */
    @Override
    @Transactional
    public String verifyOtp(OtpVerificationDto request) {
        log.info("OTP verification for: {}", request.getEmail());

        // 1. User lookup
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException(
                        "User not found: " + request.getEmail()));

        // 2. Fetch latest valid OTP — expiry enforced atomically at DB level
        OtpVerification otp = otpRepository
                .findTopByUserAndIsUsedFalseAndExpiryTimeAfterOrderByCreatedAtDesc(
                        user, LocalDateTime.now())
                .orElseThrow(() -> new AuthException(
                        "No valid OTP found. OTP may have expired — please request a new one."));

        // 3. OTP value check
        if (!otp.getOtpCode().equals(request.getOtpCode()))
            throw new AuthException("Invalid OTP. Please try again.");

        // 4. Mark OTP as consumed
        otp.setIsUsed(true);
        otpRepository.save(otp);

        // 5. Activate user account
        user.setIsVerified(true);
        userRepository.save(user);

        log.info("Account verified for userId={}", user.getUserId());
        return "Email verified successfully. Your account is pending admin approval.";
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Resolves a {@link User} from a username that may be either an email
     * address or a 10-digit mobile number.
     *
     * @param username email or mobile string from login DTO
     * @return matched {@link User}
     * @throws AuthException if no user found or credentials are invalid
     */
    private User resolveUserByUsername(String username) {
        // Simple heuristic: contains '@' → email, otherwise → mobile
        if (username.contains("@")) {
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new AuthException("Invalid credentials"));
        } else {
            String mobile = normalizeMobile(username);
            return userRepository.findByMobile(mobile)
                    .orElseThrow(() -> new AuthException("Invalid credentials"));
        }
    }

    /**
     * Normalizes a mobile number to exactly 10 digits.
     *
     * <p>Strips leading {@code +91}, {@code 91}, or {@code 0} prefixes
     * so the value always matches the {@code CHAR(10)} DB column.
     *
     * <p>Examples:
     * <pre>
     * +919876543210  →  9876543210
     *  919876543210  →  9876543210
     *    9876543210  →  9876543210
     * </pre>
     *
     * @param raw raw mobile input from DTO or login
     * @return normalized 10-digit mobile string
     */
    private String normalizeMobile(String raw) {
        if (raw == null) return null;
        // Remove all non-digit characters first (+, spaces, dashes)
        String digits = raw.replaceAll("[^0-9]", "");
        // Strip country code prefix: 91XXXXXXXXXX → XXXXXXXXXX
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        }
        return digits;
    }

    /**
     * Generates a zero-padded 6-digit numeric OTP
     * matching the {@code CHAR(6)} column in {@code otp_verification}.
     *
     * @return 6-digit OTP string e.g. "047291"
     */
    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }
}
