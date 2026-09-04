package com.smartsocietyconnect.visitor.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO used when checking out a visitor.
 *
 * <p>Check-out time should be set by the service layer using server time,
 * not accepted from the client.
 */
public record VisitorCheckOutDTO(

        /**
         * Optional security/staff remarks during check-out.
         */
        @Size(max = 255, message = "Remarks must not exceed 255 characters")
        String remarks

) {}