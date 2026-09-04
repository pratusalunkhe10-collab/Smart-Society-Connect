package com.smartsocietyconnect.auth.exception;

import java.io.Serial;

/**
 * Custom unchecked exception for authentication-related errors
 * in the Smart Society Connect application.
 *
 * <p>Thrown throughout the authentication module to signal failures including:
 * <ul>
 *     <li>Invalid credentials (wrong password, unrecognised email/mobile)</li>
 *     <li>Duplicate email or mobile during registration</li>
 *     <li>OTP validation failures (expired, already used, not found)</li>
 *     <li>Unauthorized access attempts</li>
 *     <li>Account inactive or unverified</li>
 * </ul>
 *
 * <p>Extends {@link RuntimeException} so callers are not forced to declare
 * or catch it (unchecked), keeping service and controller code clean.
 *
 * <p><b>Usage in a {@code @RestControllerAdvice}:</b>
 * <pre>{@code
 * @ExceptionHandler(AuthException.class)
 * public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
 *     return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
 *             .body(new ErrorResponse(ex.getMessage()));
 * }
 * }</pre>
 *
 * <p><b>Design note:</b> This is currently a single general-purpose exception.
 * As the module grows, consider introducing specific subclasses such as
 * {@code DuplicateEmailException}, {@code OtpExpiredException}, or
 * {@code AccountInactiveException} to allow the {@code @ExceptionHandler}
 * to map distinct HTTP status codes (409, 400, 401, 403) per error type
 * without string-matching on the message.
 *
 * @author Smart Society Connect Team
 * @version 1.0
 * @see RuntimeException
 */
public class AuthException extends RuntimeException {

    /**
     * Explicit serial version UID.
     *
     * <p>{@link RuntimeException} implements {@link java.io.Serializable}.
     * Without this constant, Java auto-generates a UID from the class
     * structure — any recompile silently changes it, breaking deserialization
     * across JVM boundaries (distributed systems, logging pipelines, etc.).
     *
     * <p>{@code @Serial} (Java 14+) provides compile-time validation
     * that this field is used correctly in a {@link java.io.Serializable} context.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code AuthException} with the specified detail message.
     *
     * <p>Use this constructor when the error originates within the auth module
     * itself and there is no underlying cause to preserve.
     *
     * <p>Example:
     * <pre>{@code
     * throw new AuthException("Email already registered: " + email);
     * }</pre>
     *
     * @param message a human-readable description of the authentication failure
     */
    public AuthException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code AuthException} with the specified detail message
     * and the underlying cause.
     *
     * <p>Use this constructor when wrapping a lower-level exception
     * (e.g. {@code DataIntegrityViolationException}, {@code JwtException})
     * so the original stack trace is preserved for debugging.
     *
     * <p><b>Always prefer this over swallowing the cause.</b> Lost root causes
     * are one of the most common sources of untraceable production bugs.
     *
     * <p>Example:
     * <pre>{@code
     * try {
     *     userRepository.save(user);
     * } catch (DataIntegrityViolationException ex) {
     *     throw new AuthException("Mobile number already registered", ex);
     * }
     * }</pre>
     *
     * @param message a human-readable description of the authentication failure
     * @param cause   the underlying exception that triggered this failure;
     *                accessible via {@link #getCause()}
     */
    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }

}