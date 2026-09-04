package com.smartsocietyconnect.auth.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.smartsocietyconnect.auth.service.EmailService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class EmailServiceImpl implements EmailService {

    private static final String EMAIL_SUBJECT = "Smart Society Connect - OTP Verification";

    // Values come from application.properties -> environment variables.
    // This keeps GitHub clones portable and prevents credentials from being hard-coded.
    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    @Override
    @Async
    public void sendOtp(String toEmail, String otpCode) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.error("RESEND_API_KEY is not configured. OTP email cannot be sent to: {}", toEmail);
            throw new IllegalStateException(
                    "Email service is not configured. Set RESEND_API_KEY in the application environment.");
        }

        log.info("Sending OTP email through Resend to: {}", toEmail);
        try {
            Resend resend = new Resend(resendApiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("Smart Society Connect <" + fromEmail + ">")
                    .to(toEmail)
                    .subject(EMAIL_SUBJECT)
                    .html(buildOtpEmailBody(otpCode))
                    .build();
            CreateEmailResponse response = resend.emails().send(params);
            log.info("OTP email accepted by Resend for: {} (email id: {})", toEmail, response.getId());
        } catch (Exception exception) {
            log.error("Failed to send OTP email through Resend to: {}", toEmail, exception);
            throw new RuntimeException("Unable to send OTP email", exception);
        }
    }

    private String buildOtpEmailBody(String otpCode) {
        return """
                <!doctype html>
                <html lang="en">
                <body style="font-family:Arial,sans-serif;background:#fff7f5;padding:30px;color:#1f2937;">
                  <div style="max-width:600px;margin:auto;background:white;border:1px solid #ffe1dc;border-radius:16px;overflow:hidden;">
                    <div style="background:#be123c;color:white;padding:25px 32px;">
                      <div style="font-size:13px;font-weight:700;letter-spacing:1.5px;">SMART SOCIETY CONNECT</div>
                      <h2 style="margin:10px 0 0;">Confirm your email address</h2>
                    </div>
                    <div style="padding:32px;">
                      <p>Thank you for creating your Smart Society Connect account.</p>
                      <p>Use the verification code below to confirm your email address:</p>
                      <div style="margin:24px 0;padding:18px;text-align:center;background:#fff1f2;border:1px solid #fecaca;border-radius:12px;">
                        <div style="font-size:12px;font-weight:700;color:#9f1239;">YOUR VERIFICATION CODE</div>
                        <div style="margin-top:8px;font-size:32px;font-weight:700;letter-spacing:8px;color:#9f1239;">%s</div>
                      </div>
                      <p>This code expires in <strong>5 minutes</strong> and can be used only once.</p>
                      <p>For your security, never share this code with anyone.</p>
                      <p>If you did not request this account, you can safely ignore this email.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(otpCode);
    }
}
