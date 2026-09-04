package com.smartsocietyconnect.billing.repository;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smartsocietyconnect.billing.entity.Billing;
import com.smartsocietyconnect.billing.enums.BillingStatus;
import com.smartsocietyconnect.resident.entity.Resident;

/**
 * Repository for Billing entity database operations.
 *
 * <p>Extends {@link JpaRepository} to provide standard CRUD methods:
 * <ul>
 *     <li>{@code save()}</li>
 *     <li>{@code findById()}</li>
 *     <li>{@code findAll()}</li>
 *     <li>{@code delete()}</li>
 * </ul>
 *
 * <p>The primary key type is {@link String} because {@code billing_id}
 * is a UUID string.
 */
@Repository
public interface BillingRepository extends JpaRepository<Billing, String> {

    // ==========================================================================
    // 1. Find By Billing ID
    // ==========================================================================

    /**
     * Find billing record by UUID billing ID.
     *
     * <p>This method is similar to {@code findById()}, but improves
     * readability in the service layer.
     *
     * @param billingId billing UUID
     * @return optional billing record
     */
    Optional<Billing> findByBillingId(String billingId);

    // ==========================================================================
    // 2. Duplicate Billing Validation
    // ==========================================================================

    /**
     * Check whether a bill already exists for the same resident, month, and year.
     *
     * <p>Used before creating a new bill to prevent duplicate monthly bills.
     *
     * @param resident resident entity
     * @param billingMonth month number from 1 to 12
     * @param billingYear billing year
     * @return true if duplicate bill exists
     */
    boolean existsByResidentAndBillingMonthAndBillingYear(
            Resident resident,
            Integer billingMonth,
            Integer billingYear
    );

    /**
     * Find a bill by resident, billing month, and billing year.
     *
     * <p>Used during bill update to check whether another bill already exists
     * for the same resident/month/year combination.
     *
     * @param resident resident entity
     * @param billingMonth month number from 1 to 12
     * @param billingYear billing year
     * @return optional billing record
     */
    Optional<Billing> findByResidentAndBillingMonthAndBillingYear(
            Resident resident,
            Integer billingMonth,
            Integer billingYear
    );

    // ==========================================================================
    // 3. Resident-Based Queries
    // ==========================================================================

    /**
     * Get all bills for one resident.
     *
     * @param resident resident entity
     * @return resident billing records
     */
    List<Billing> findByResident(Resident resident);

    boolean existsByBillingIdAndResidentUserEmailIgnoreCase(
            String billingId,
            String email
    );

    /**
     * Get all bills for one resident ordered by latest billing period first.
     *
     * @param resident resident entity
     * @return ordered resident billing records
     */
    List<Billing> findByResidentOrderByBillingYearDescBillingMonthDesc(
            Resident resident
    );

    // ==========================================================================
    // 4. Status-Based Queries
    // ==========================================================================

    /**
     * Get bills by billing status.
     *
     * @param status billing status
     * @return bills matching status
     */
    List<Billing> findByStatus(BillingStatus status);

    List<Billing> findByDueDateAndStatusIn(LocalDate dueDate, List<BillingStatus> statuses);

    // ==========================================================================
    // 5. Billing Period Queries
    // ==========================================================================

    /**
     * Get bills by billing month.
     *
     * @param billingMonth month number from 1 to 12
     * @return bills generated for the given month
     */
    List<Billing> findByBillingMonth(Integer billingMonth);

    /**
     * Get bills by billing year.
     *
     * @param billingYear billing year
     * @return bills generated for the given year
     */
    List<Billing> findByBillingYear(Integer billingYear);

    /**
     * Get bills by billing month and billing year.
     *
     * @param billingMonth month number from 1 to 12
     * @param billingYear billing year
     * @return bills generated for the given month and year
     */
    List<Billing> findByBillingMonthAndBillingYear(
            Integer billingMonth,
            Integer billingYear
    );

    // ==========================================================================
    // 6. Ordered List Queries
    // ==========================================================================

    /**
     * Get all bills ordered by latest billing period first.
     *
     * <p>Used by {@code getAllBills()} in the service layer.
     *
     * @return all billing records ordered by billing year and month descending
     */
    List<Billing> findAllByOrderByBillingYearDescBillingMonthDesc();
    
	 // ==========================================================================
	 // Dashboard Statistics
	 // ==========================================================================
	
	 long countByStatus(BillingStatus status);
	
	 long countByResidentResidentId(Integer residentId);
	
	 long countByResidentResidentIdAndStatus(
	         Integer residentId,
	         BillingStatus status
	 );
	
	 @Query("""
	        SELECT COALESCE(SUM(b.totalAmount), 0)
	        FROM Billing b
	        WHERE b.status IN (
	             com.smartsocietyconnect.billing.enums.BillingStatus.PENDING,
	             com.smartsocietyconnect.billing.enums.BillingStatus.PARTIALLY_PAID,
	             com.smartsocietyconnect.billing.enums.BillingStatus.OVERDUE
	        )
	        """)
	 BigDecimal getOutstandingAmount();
	
	 @Query("""
	        SELECT COALESCE(SUM(b.totalAmount), 0)
	        FROM Billing b
	        WHERE b.resident.residentId = :residentId
	          AND b.status IN (
	             com.smartsocietyconnect.billing.enums.BillingStatus.PENDING,
	             com.smartsocietyconnect.billing.enums.BillingStatus.PARTIALLY_PAID,
	             com.smartsocietyconnect.billing.enums.BillingStatus.OVERDUE
	        )
	        """)
	 BigDecimal getOutstandingAmountByResident(
	         @Param("residentId") Integer residentId
	 );
}
