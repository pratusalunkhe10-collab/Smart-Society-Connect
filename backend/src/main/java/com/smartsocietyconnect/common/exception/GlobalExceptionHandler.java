package com.smartsocietyconnect.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.smartsocietyconnect.auth.exception.AuthException;
import com.smartsocietyconnect.billing.exception.BillingException;
import com.smartsocietyconnect.billing.exception.BillingNotFoundException;
import com.smartsocietyconnect.billing.exception.DuplicateBillingException;
import com.smartsocietyconnect.billing.exception.InvalidBillingStatusException;
import com.smartsocietyconnect.billing.exception.PaymentException;
import com.smartsocietyconnect.billing.exception.PaymentNotFoundException;
import com.smartsocietyconnect.complaint.exception.ComplaintNotFoundException;
import com.smartsocietyconnect.complaint.exception.ComplaintOperationException;
import com.smartsocietyconnect.complaint.exception.DuplicateComplaintException;
import com.smartsocietyconnect.complaint.exception.InvalidComplaintStatusException;
import com.smartsocietyconnect.meeting.exception.AttendanceAlreadySubmittedException;
import com.smartsocietyconnect.meeting.exception.DuplicateMeetingException;
import com.smartsocietyconnect.meeting.exception.InvalidMeetingStatusException;
import com.smartsocietyconnect.meeting.exception.MeetingAlreadyCompletedException;
import com.smartsocietyconnect.meeting.exception.MeetingCancelledException;
import com.smartsocietyconnect.meeting.exception.MeetingException;
import com.smartsocietyconnect.meeting.exception.MeetingNotFoundException;
import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.visitor.exception.DuplicateVisitorException;
import com.smartsocietyconnect.visitor.exception.InvalidVisitorStatusException;
import com.smartsocietyconnect.visitor.exception.VisitorException;
import com.smartsocietyconnect.visitor.exception.VisitorNotFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for the Smart Society Connect application.
 *
 * <p>Intercepts exceptions thrown from REST controllers and converts them into
 * consistent HTTP error responses.
 *
 * <p><b>Handler order matters:</b>
 * Specific exceptions must be declared before generic exceptions such as
 * {@link RuntimeException} and {@link Exception}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==========================================================================
    // 1. Validation Exception - 400 Bad Request
    // ==========================================================================

    /**
     * Handles validation errors from {@code @Valid} request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fieldErrors = new HashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        fieldErrors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        Map<String, Object> response = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed"
        );

        response.put("errors", fieldErrors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ==========================================================================
    // 2. ConstraintViolationException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles validation errors from path variables and request parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 3. Missing Request Parameter - 400 Bad Request
    // ==========================================================================

    /**
     * Handles missing required query/form parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestParameterException(
            MissingServletRequestParameterException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Missing required parameter: " + exception.getParameterName()
        );
    }

    // ==========================================================================
    // 4. Missing Multipart Part - 400 Bad Request
    // ==========================================================================

    /**
     * Handles missing required multipart parts such as uploaded files.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestPartException(
            MissingServletRequestPartException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Missing required multipart part: "
                        + exception.getRequestPartName()
        );
    }

    // ==========================================================================
    // 5. JSON Parse Exception - 400 Bad Request
    // ==========================================================================

    /**
     * Handles malformed JSON or invalid enum values in request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMostSpecificCause().getMessage()
        );
    }

    // ==========================================================================
    // 6. Type Mismatch Exception - 400 Bad Request
    // ==========================================================================

    /**
     * Handles invalid path variable or query parameter type conversion.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + exception.getName()
        );
    }

    // ==========================================================================
    // 7. Multipart Exception - 400 Bad Request
    // ==========================================================================

    /**
     * Handles malformed multipart/form-data requests.
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(
            MultipartException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid multipart request: " + exception.getMessage()
        );
    }

    // ==========================================================================
    // 8. Max Upload Size Exception - 400 Bad Request
    // ==========================================================================

    /**
     * Handles uploaded files exceeding configured max file size.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Uploaded file is too large"
        );
    }

    // ==========================================================================
    // 9. Unsupported Media Type - 415 Unsupported Media Type
    // ==========================================================================

    /**
     * Handles unsupported content types.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException exception
    ) {
        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type: " + exception.getContentType()
        );
    }

    // ==========================================================================
    // 10. AuthException - 401 Unauthorized
    // ==========================================================================

    /**
     * Handles authentication-module business failures.
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(
            AuthException exception
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 11. ResidentException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles resident-module business failures.
     */
    @ExceptionHandler(ResidentException.class)
    public ResponseEntity<Map<String, Object>> handleResidentException(
            ResidentException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 12. VisitorNotFoundException - 404 Not Found
    // ==========================================================================

    /**
     * Handles visitor not found errors.
     */
    @ExceptionHandler(VisitorNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleVisitorNotFoundException(
            VisitorNotFoundException exception
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 13. InvalidVisitorStatusException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles invalid visitor workflow transitions.
     */
    @ExceptionHandler(InvalidVisitorStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidVisitorStatusException(
            InvalidVisitorStatusException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 14. DuplicateVisitorException - 409 Conflict
    // ==========================================================================

    /**
     * Handles duplicate visitor business errors.
     */
    @ExceptionHandler(DuplicateVisitorException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateVisitorException(
            DuplicateVisitorException exception
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 15. VisitorException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles general visitor-module business errors.
     */
    @ExceptionHandler(VisitorException.class)
    public ResponseEntity<Map<String, Object>> handleVisitorException(
            VisitorException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 16. ComplaintNotFoundException - 404 Not Found
    // ==========================================================================

    /**
     * Handles complaint not found errors.
     */
    @ExceptionHandler(ComplaintNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleComplaintNotFoundException(
            ComplaintNotFoundException exception
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 17. InvalidComplaintStatusException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles invalid complaint workflow transitions.
     */
    @ExceptionHandler(InvalidComplaintStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidComplaintStatusException(
            InvalidComplaintStatusException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 18. DuplicateComplaintException - 409 Conflict
    // ==========================================================================

    /**
     * Handles duplicate complaint number conflicts.
     */
    @ExceptionHandler(DuplicateComplaintException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateComplaintException(
            DuplicateComplaintException exception
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 19. ComplaintOperationException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles complaint-module business rule violations.
     */
    @ExceptionHandler(ComplaintOperationException.class)
    public ResponseEntity<Map<String, Object>> handleComplaintOperationException(
            ComplaintOperationException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 20. BillingNotFoundException - 404 Not Found
    // ==========================================================================

    /**
     * Handles billing record not found errors.
     */
    @ExceptionHandler(BillingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBillingNotFoundException(
            BillingNotFoundException exception
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 21. DuplicateBillingException - 409 Conflict
    // ==========================================================================

    /**
     * Handles duplicate monthly bill conflicts.
     */
    @ExceptionHandler(DuplicateBillingException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateBillingException(
            DuplicateBillingException exception
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 22. InvalidBillingStatusException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles invalid billing status operations.
     */
    @ExceptionHandler(InvalidBillingStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBillingStatusException(
            InvalidBillingStatusException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 23. BillingException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles general billing-module business errors.
     */
    @ExceptionHandler(BillingException.class)
    public ResponseEntity<Map<String, Object>> handleBillingException(
            BillingException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 24. PaymentNotFoundException - 404 Not Found
    // ==========================================================================

    /**
     * Handles payment record not found errors.
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotFoundException(
            PaymentNotFoundException exception
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 25. PaymentException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles payment-module business errors.
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentException(
            PaymentException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 26. MeetingNotFoundException - 404 Not Found
    // ==========================================================================

    /**
     * Handles meeting not found errors.
     */
    @ExceptionHandler(MeetingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMeetingNotFoundException(
            MeetingNotFoundException exception
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 27. DuplicateMeetingException - 409 Conflict
    // ==========================================================================

    /**
     * Handles duplicate meeting conflicts.
     */
    @ExceptionHandler(DuplicateMeetingException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateMeetingException(
            DuplicateMeetingException exception
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 28. MeetingAlreadyCompletedException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles invalid operations attempted on completed meetings.
     */
    @ExceptionHandler(MeetingAlreadyCompletedException.class)
    public ResponseEntity<Map<String, Object>> handleMeetingAlreadyCompletedException(
            MeetingAlreadyCompletedException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 29. MeetingCancelledException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles invalid operations attempted on cancelled meetings.
     */
    @ExceptionHandler(MeetingCancelledException.class)
    public ResponseEntity<Map<String, Object>> handleMeetingCancelledException(
            MeetingCancelledException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 30. AttendanceAlreadySubmittedException - 409 Conflict
    // ==========================================================================

    /**
     * Handles duplicate RSVP submission attempts.
     */
    @ExceptionHandler(AttendanceAlreadySubmittedException.class)
    public ResponseEntity<Map<String, Object>> handleAttendanceAlreadySubmittedException(
            AttendanceAlreadySubmittedException exception
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 31. InvalidMeetingStatusException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles invalid meeting status transitions.
     */
    @ExceptionHandler(InvalidMeetingStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMeetingStatusException(
            InvalidMeetingStatusException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 32. MeetingException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles general meeting-module business errors.
     */
    @ExceptionHandler(MeetingException.class)
    public ResponseEntity<Map<String, Object>> handleMeetingException(
            MeetingException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 33. RuntimeException - 400 Bad Request
    // ==========================================================================

    /**
     * Handles unchecked exceptions not covered by specific handlers.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException exception
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // ==========================================================================
    // 34. Exception - 500 Internal Server Error
    // ==========================================================================

    /**
     * Handles unexpected system errors as the final fallback.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception exception
    ) {
        log.error("Unhandled exception occurred", exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    /**
     * Build a {@link ResponseEntity} using the standard error response body.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message
    ) {
        return new ResponseEntity<>(
                buildErrorResponse(status, message),
                status
        );
    }

    /**
     * Build the standard error response body.
     *
     * <p>Response format:
     * <pre>
     * {
     *   "timestamp": "...",
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "..."
     * }
     * </pre>
     */
    private Map<String, Object> buildErrorResponse(
            HttpStatus status,
            String message
    ) {
        Map<String, Object> response = new HashMap<>();

        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);

        return response;
    }
}