package com.smartsocietyconnect.visitor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO used to approve or reject a visitor request.
 *
 * <p>Used by an authorized resident, admin, or security workflow depending
 * on your application rules.
 */
public record VisitorApprovalDTO(

        /**
         * Approval decision.
         *
         * <p>{@code true} means approve, {@code false} means reject.
         */
        @NotNull(message = "Approval decision is required")
        Boolean approved,

        /**
         * Optional remarks explaining approval or rejection.
         */
        @Size(max = 255, message = "Remarks must not exceed 255 characters")
        String remarks

) {}