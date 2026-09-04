package com.smartsocietyconnect.billing.enums;

/**
 * Represents the processing status of a payment transaction.
 *
 * <p>This enum is different from {@link BillingStatus}.
 * {@code BillingStatus} belongs to the bill, while {@code PaymentStatus}
 * belongs to an individual payment transaction.
 *
 * <p>Status meaning:
 * <ul>
 *     <li>{@code SUCCESS} - Payment completed successfully.</li>
 *     <li>{@code FAILED} - Payment attempt failed.</li>
 *     <li>{@code PENDING} - Payment is initiated but confirmation is pending.</li>
 * </ul>
 */
public enum PaymentStatus {

    SUCCESS,

    FAILED,

    PENDING
}