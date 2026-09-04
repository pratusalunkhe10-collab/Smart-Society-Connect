package com.smartsocietyconnect.auth.service;

/**
 * Service interface for email operations.
 *
 * <p>Handles:
 * <ul>
 *     <li>OTP email delivery</li>
 *     <li>Password reset emails</li>
 *     <li>Account notifications</li>
 * </ul>
 *
 * @author Smart Society Connect Team
 * @version 1.0
 */
public interface EmailService {

    /**
     * Sends OTP to the specified email address.
     *
     * @param toEmail recipient email address
     * @param otpCode generated OTP code
     */
    void sendOtp(String toEmail, String otpCode);

}