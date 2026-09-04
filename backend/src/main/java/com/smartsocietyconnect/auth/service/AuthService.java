package com.smartsocietyconnect.auth.service;

import com.smartsocietyconnect.auth.dto.AuthResponseDto;
import com.smartsocietyconnect.auth.dto.LoginRequestDto;
import com.smartsocietyconnect.auth.dto.OtpVerificationDto;
import com.smartsocietyconnect.auth.dto.RegisterRequestDto;

/**
 * Service interface for authentication operations.
 *
 * <p>Defines business logic contracts for:
 * <ul>
 *     <li>User registration</li>
 *     <li>User login</li>
 *     <li>OTP verification</li>
 *     <li>OTP resend</li>
 *     <li>Logout</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
public interface AuthService {

    /**
     * Registers a new user and dispatches OTP to their email.
     *
     * <p>Account remains inactive ({@code is_verified = false})
     * until OTP is verified.
     *
     * @param request registration request DTO
     * @return {@link AuthResponseDto} with email and fullName (no token yet)
     */
    AuthResponseDto registerUser(RegisterRequestDto request);

    /**
     * Authenticates user by email or mobile and issues a JWT token.
     *
     * <p>Only succeeds if account is active and OTP-verified.
     *
     * @param request login request DTO (username + password)
     * @return {@link AuthResponseDto} with JWT token, userId, fullName, roleName
     */
    AuthResponseDto loginUser(LoginRequestDto request);

    /**
     * Verifies OTP and activates the user account.
     *
     * <p>Marks OTP as {@code is_used = true} and
     * sets {@code is_verified = true} on the user.
     *
     * @param request OTP verification DTO (email + otpCode)
     * @return success message string
     */
    String verifyOtp(OtpVerificationDto request);

    /**
     * Resends a fresh OTP to the user's registered email.
     *
     * <p>Only allowed if account is not yet verified.
     * Generates a new OTP record in {@code otp_verification} table.
     *
     * @param email the registered email address
     * @return {@link AuthResponseDto} with email confirmation (no token)
     */
    AuthResponseDto resendOtp(String email);   // ✅ NEW

    /**
     * Logs out a user by invalidating their JWT session.
     *
     * <p>Finds the session in {@code user_sessions} table by token,
     * sets {@code is_logged_out = true} and records {@code logout_time}.
     *
     * @param token the raw JWT token string to invalidate
     * @return {@link AuthResponseDto} with logout confirmation
     */
    AuthResponseDto logout(String token);      // ✅ NEW
}