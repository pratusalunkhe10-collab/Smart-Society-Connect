package com.smartsocietyconnect.billing.enums;

/**
 * Represents the current payment state of a generated bill.
 *
 * <p>This enum should match the database ENUM values used in the billing table.
 *
 * <p>Status meaning:
 * <ul>
 *     <li>{@code PENDING} - Bill is generated but no payment has been made.</li>
 *     <li>{@code PARTIALLY_PAID} - Some amount has been paid, but balance is still pending.</li>
 *     <li>{@code PAID} - Full bill amount has been paid.</li>
 *     <li>{@code OVERDUE} - Bill due date has passed and payment is still pending.</li>
 *     <li>{@code CANCELLED} - Bill has been cancelled and should not be collected.</li>
 * </ul>
 */
public enum BillingStatus {

    PENDING,

    PARTIALLY_PAID,

    PAID,

    OVERDUE,

    CANCELLED
}