package com.smartsocietyconnect.visitor.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO used when checking in an approved visitor.
 *
 * <p>Check-in time should be set by the service layer using server time,
 * not accepted from the client.
 */
public record VisitorCheckInDTO(

        /**
         * Optional vehicle number captured at gate/security desk.
         */
        @Size(max = 20, message = "Vehicle number must not exceed 20 characters")
        String vehicleNumber,

        /**
         * Optional security/staff remarks during check-in.
         */
        @Size(max = 255, message = "Remarks must not exceed 255 characters")
        String remarks

) {}