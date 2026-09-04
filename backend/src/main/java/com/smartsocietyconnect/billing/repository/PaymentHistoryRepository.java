package com.smartsocietyconnect.billing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartsocietyconnect.billing.entity.Billing;
import com.smartsocietyconnect.billing.entity.PaymentHistory;
import com.smartsocietyconnect.billing.enums.PaymentStatus;

/**
 * Repository for PaymentHistory entity database operations.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD operations:
 * <ul>
 *     <li>{@code save()}</li>
 *     <li>{@code findById()}</li>
 *     <li>{@code findAll()}</li>
 *     <li>{@code delete()}</li>
 * </ul>
 *
 * <p>The primary key type is {@link String} because {@code payment_id}
 * is UUID-based.
 */
@Repository
public interface PaymentHistoryRepository
        extends JpaRepository<PaymentHistory, String> {

    // ==========================================================================
    // 1. Find By Payment ID
    // ==========================================================================

    /**
     * Find payment record by UUID payment ID.
     *
     * <p>This method is similar to {@code findById()}, but improves readability
     * in the service layer.
     *
     * @param paymentId payment UUID
     * @return optional payment history record
     */
    Optional<PaymentHistory> findByPaymentId(String paymentId);

    boolean existsByPaymentIdAndBillingResidentUserEmailIgnoreCase(
            String paymentId,
            String email
    );

    // ==========================================================================
    // 2. Billing-Based Queries
    // ==========================================================================

    /**
     * Get all payment records for a bill.
     *
     * <p>Used when calculating already paid amount for a bill.
     *
     * @param billing billing entity
     * @return payment records linked to the bill
     */
    List<PaymentHistory> findByBilling(Billing billing);

    /**
     * Get all payment records for a bill ordered by latest payment first.
     *
     * <p>Used when showing payment history to admin/resident.
     *
     * @param billing billing entity
     * @return ordered payment records linked to the bill
     */
    List<PaymentHistory> findByBillingOrderByPaymentDateDesc(Billing billing);

    // ==========================================================================
    // 3. Payment Status Queries
    // ==========================================================================

    /**
     * Get payments by payment processing status.
     *
     * @param paymentStatus payment status
     * @return payment records matching the status
     */
    List<PaymentHistory> findByPaymentStatus(PaymentStatus paymentStatus);

    // ==========================================================================
    // 4. Transaction Reference Queries
    // ==========================================================================

    /**
     * Find payment using external transaction reference.
     *
     * <p>Useful for checking whether a UPI, card, cheque, or bank transfer
     * reference already exists.
     *
     * @param transactionReference external payment reference
     * @return optional payment history record
     */
    Optional<PaymentHistory> findByTransactionReference(
            String transactionReference
    );

    /**
     * Check whether a transaction reference already exists.
     *
     * <p>Used before saving payment to prevent duplicate transaction entries.
     *
     * @param transactionReference external payment reference
     * @return true if transaction reference already exists
     */
    boolean existsByTransactionReference(String transactionReference);

    // ==========================================================================
    // 5. Ordered List Queries
    // ==========================================================================

    /**
     * Get all payment records ordered by latest payment first.
     *
     * <p>Used by {@code PaymentServiceImpl#getAllPayments()}.
     *
     * @return all payment history records ordered by payment date descending
     */
    List<PaymentHistory> findAllByOrderByPaymentDateDesc();
    
	 // ==========================================================================
	 // Dashboard Statistics
	 // ==========================================================================
	
	 @Query("""
	        SELECT COALESCE(SUM(p.amountPaid), 0)
	        FROM PaymentHistory p
	        WHERE p.paymentStatus = com.smartsocietyconnect.billing.enums.PaymentStatus.SUCCESS
	        """)
	 BigDecimal getTotalCollection();
	
	 @Query("""
	        SELECT COALESCE(SUM(p.amountPaid), 0)
	        FROM PaymentHistory p
	        WHERE p.paymentStatus = com.smartsocietyconnect.billing.enums.PaymentStatus.SUCCESS
	          AND p.paymentDate >= :start
	          AND p.paymentDate < :end
	        """)
	 BigDecimal getCollectionBetween(
	         @Param("start") LocalDateTime start,
	         @Param("end") LocalDateTime end
	 );
}
