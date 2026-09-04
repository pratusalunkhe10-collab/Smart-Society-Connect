package com.smartsocietyconnect.billing.enums;

/**
 * Represents the mode used by a resident to make a payment.
 *
 * <p>This enum helps keep payment mode values type-safe across the application.
 *
 * <p>Examples:
 * <ul>
 *     <li>{@code UPI} - Google Pay, PhonePe, Paytm, BHIM, etc.</li>
 *     <li>{@code CARD} - Debit or credit card payment.</li>
 *     <li>{@code BANK_TRANSFER} - NEFT, RTGS, IMPS, or direct bank transfer.</li>
 *     <li>{@code CHEQUE} - Cheque payment submitted to society office.</li>
 *     <li>{@code CASH} - Cash payment collected manually.</li>
 * </ul>
 */
public enum PaymentMode {

    UPI,

    CARD,

    BANK_TRANSFER,

    CHEQUE,

    CASH
}