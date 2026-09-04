package com.smartsocietyconnect.billing.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartsocietyconnect.billing.dto.BillingRequest;
import com.smartsocietyconnect.billing.dto.BillingResponse;
import com.smartsocietyconnect.billing.entity.Billing;
import com.smartsocietyconnect.billing.enums.BillingStatus;
import com.smartsocietyconnect.billing.exception.BillingNotFoundException;
import com.smartsocietyconnect.billing.exception.DuplicateBillingException;
import com.smartsocietyconnect.billing.exception.InvalidBillingStatusException;
import com.smartsocietyconnect.billing.mapper.BillingMapper;
import com.smartsocietyconnect.billing.repository.BillingRepository;
import com.smartsocietyconnect.billing.service.BillingService;
import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.resident.repository.ResidentRepository;
import com.smartsocietyconnect.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for Billing module operations.
 *
 * <p>This class contains the business logic for:
 * <ul>
 *     <li>Generating monthly bills</li>
 *     <li>Updating bill details</li>
 *     <li>Deleting unpaid bills</li>
 *     <li>Fetching bills</li>
 *     <li>Marking bills as overdue</li>
 * </ul>
 *
 * <p>Controller classes should not contain billing business rules.
 * Those rules belong here in the service layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BillingServiceImpl implements BillingService {

    private final BillingRepository billingRepository;
    private final ResidentRepository residentRepository;
    private final BillingMapper billingMapper;
    private final NotificationService notificationService;

    // ==========================================================================
    // 1. Create Bill
    // ==========================================================================

    /**
     * Generate a new monthly bill for a resident.
     *
     * <p>Business rules:
     * <ul>
     *     <li>Resident must exist.</li>
     *     <li>Duplicate bill for same resident/month/year is not allowed.</li>
     *     <li>Total amount is calculated on backend.</li>
     *     <li>New bill starts as {@code PENDING} by default.</li>
     * </ul>
     *
     * @param request billing creation request
     * @return created billing response
     */
    @Override
    public BillingResponse createBill(BillingRequest request) {

        Resident resident = getResident(request.getResidentId());

        validateDuplicateBill(
                resident,
                request.getBillingMonth(),
                request.getBillingYear(),
                null
        );

        Billing billing = billingMapper.toEntity(request, resident);

        normalizeChargeFields(billing);
        billing.setTotalAmount(calculateTotalAmount(billing));

        Billing savedBilling = billingRepository.save(billing);

        notificationService.notifyUser(resident.getUser(), "BILLING", "New maintenance bill generated",
                "Your bill for " + savedBilling.getBillingMonth() + "/" + savedBilling.getBillingYear()
                        + " is ₹" + savedBilling.getTotalAmount() + ". Due date: " + savedBilling.getDueDate() + ".",
                "/app/billing");

        log.info(
                "Bill created successfully. billingId={}, residentId={}, month={}, year={}",
                savedBilling.getBillingId(),
                resident.getResidentId(),
                savedBilling.getBillingMonth(),
                savedBilling.getBillingYear()
        );

        return billingMapper.toResponse(savedBilling);
    }

    // ==========================================================================
    // 2. Update Bill
    // ==========================================================================

    /**
     * Update an existing billing record.
     *
     * <p>Business rules:
     * <ul>
     *     <li>Bill must exist.</li>
     *     <li>Resident must exist.</li>
     *     <li>{@code PAID}, {@code PARTIALLY_PAID}, and {@code CANCELLED}
     *         bills cannot be edited.</li>
     *     <li>Duplicate resident/month/year combination is not allowed.</li>
     *     <li>Total amount is recalculated after update.</li>
     * </ul>
     *
     * @param billingId billing UUID
     * @param request updated billing request
     * @return updated billing response
     */
    @Override
    public BillingResponse updateBill(
            String billingId,
            BillingRequest request
    ) {

        Billing billing = getBilling(billingId);

        validateBillCanBeUpdated(billing);

        Resident resident = getResident(request.getResidentId());

        validateDuplicateBill(
                resident,
                request.getBillingMonth(),
                request.getBillingYear(),
                billing.getBillingId()
        );

        billing.setResident(resident);
        billing.setBillingMonth(request.getBillingMonth());
        billing.setBillingYear(request.getBillingYear());
        billing.setMaintenanceAmount(request.getMaintenanceAmount());
        billing.setWaterCharge(request.getWaterCharge());
        billing.setElectricityCharge(request.getElectricityCharge());
        billing.setParkingCharge(request.getParkingCharge());
        billing.setPenalty(request.getPenalty());
        billing.setOtherCharge(request.getOtherCharge());
        billing.setDueDate(request.getDueDate());
        billing.setRemarks(request.getRemarks());

        normalizeChargeFields(billing);
        billing.setTotalAmount(calculateTotalAmount(billing));

        Billing updatedBilling = billingRepository.save(billing);

        log.info("Bill updated successfully. billingId={}", updatedBilling.getBillingId());

        return billingMapper.toResponse(updatedBilling);
    }

    // ==========================================================================
    // 3. Delete Bill
    // ==========================================================================

    /**
     * Delete a billing record.
     *
     * <p>Business rule:
     * Paid and partially paid bills must not be deleted because they are
     * financial records and may have payment history.
     *
     * @param billingId billing UUID
     */
    @Override
    public void deleteBill(String billingId) {

        Billing billing = getBilling(billingId);

        validateBillCanBeDeleted(billing);

        billingRepository.delete(billing);

        log.info("Bill deleted successfully. billingId={}", billingId);
    }

    // ==========================================================================
    // 4. Get Bill By ID
    // ==========================================================================

    /**
     * Get billing record by billing ID.
     *
     * @param billingId billing UUID
     * @return billing response
     */
    @Override
    @Transactional(readOnly = true)
    public BillingResponse getBillById(String billingId) {

        Billing billing = getBilling(billingId);

        return billingMapper.toResponse(billing);
    }

    // ==========================================================================
    // 5. Get All Bills
    // ==========================================================================

    /**
     * Get all bills ordered by latest billing period first.
     *
     * @return billing response list
     */
    @Override
    @Transactional(readOnly = true)
    public List<BillingResponse> getAllBills() {

        return billingRepository
                .findAllByOrderByBillingYearDescBillingMonthDesc()
                .stream()
                .map(billingMapper::toResponse)
                .toList();
    }

    // ==========================================================================
    // 6. Get Bills By Resident
    // ==========================================================================

    /**
     * Get all bills for a resident.
     *
     * <p>Resident is validated first so invalid resident IDs return a clear
     * resident-related error.
     *
     * @param residentId resident primary key
     * @return resident billing response list
     */
    @Override
    @Transactional(readOnly = true)
    public List<BillingResponse> getBillsByResident(Integer residentId) {

        Resident resident = getResident(residentId);

        return billingRepository
                .findByResidentOrderByBillingYearDescBillingMonthDesc(resident)
                .stream()
                .map(billingMapper::toResponse)
                .toList();
    }

    // ==========================================================================
    // 7. Get Bills By Status
    // ==========================================================================

    /**
     * Get bills filtered by billing status.
     *
     * @param status billing status
     * @return billing response list
     */
    @Override
    @Transactional(readOnly = true)
    public List<BillingResponse> getBillsByStatus(BillingStatus status) {

        return billingRepository
                .findByStatus(status)
                .stream()
                .map(billingMapper::toResponse)
                .toList();
    }

    // ==========================================================================
    // 8. Mark Bill As Overdue
    // ==========================================================================

    /**
     * Mark a pending bill as overdue.
     *
     * <p>Business rules:
     * <ul>
     *     <li>Only {@code PENDING} bills can become {@code OVERDUE}.</li>
     *     <li>Due date must already be expired.</li>
     *     <li>{@code PAID}, {@code PARTIALLY_PAID}, and {@code CANCELLED}
     *         bills must never become overdue.</li>
     * </ul>
     *
     * @param billingId billing UUID
     * @return updated billing response
     */
    @Override
    public BillingResponse markAsOverdue(String billingId) {

        Billing billing = getBilling(billingId);

        if (billing.getStatus() != BillingStatus.PENDING) {
            throw new InvalidBillingStatusException(
                    "Only pending bills can be marked as overdue."
            );
        }

        if (!billing.getDueDate().isBefore(LocalDate.now())) {
            throw new InvalidBillingStatusException(
                    "Billing due date has not yet expired."
            );
        }

        billing.setStatus(BillingStatus.OVERDUE);

        Billing updatedBilling = billingRepository.save(billing);

        notificationService.notifyUser(billing.getResident().getUser(), "BILLING", "Maintenance bill is overdue",
                "Your bill of ₹" + billing.getTotalAmount() + " was due on " + billing.getDueDate()
                        + ". Please make payment at the earliest.",
                "/app/billing");

        log.info("Bill marked as overdue. billingId={}", billingId);

        return billingMapper.toResponse(updatedBilling);
    }

    // ==========================================================================
    // 9. Helper - Fetch Resident
    // ==========================================================================

    /**
     * Fetch resident by ID.
     *
     * @param residentId resident primary key
     * @return resident entity
     */
    private Resident getResident(Integer residentId) {

        return residentRepository.findById(residentId)
                .orElseThrow(() ->
                        new ResidentException(
                                "Resident not found with ID: " + residentId
                        )
                );
    }

    // ==========================================================================
    // 10. Helper - Fetch Billing
    // ==========================================================================

    /**
     * Fetch billing record by billing ID.
     *
     * @param billingId billing UUID
     * @return billing entity
     */
    private Billing getBilling(String billingId) {

        return billingRepository.findByBillingId(billingId)
                .orElseThrow(() ->
                        new BillingNotFoundException(billingId)
                );
    }

    // ==========================================================================
    // 11. Helper - Duplicate Bill Validation
    // ==========================================================================

    /**
     * Validate that another bill does not already exist for the same resident,
     * month, and year.
     *
     * <p>When creating a bill, {@code currentBillingId} should be {@code null}.
     * When updating a bill, {@code currentBillingId} is used to ignore the
     * current record during duplicate checking.
     *
     * @param resident resident entity
     * @param billingMonth billing month
     * @param billingYear billing year
     * @param currentBillingId current billing ID during update, or null during create
     */
    private void validateDuplicateBill(
            Resident resident,
            Integer billingMonth,
            Integer billingYear,
            String currentBillingId
    ) {

        billingRepository
                .findByResidentAndBillingMonthAndBillingYear(
                        resident,
                        billingMonth,
                        billingYear
                )
                .filter(existingBill ->
                        currentBillingId == null
                                || !existingBill.getBillingId().equals(currentBillingId)
                )
                .ifPresent(existingBill -> {
                    throw new DuplicateBillingException(
                            resident.getResidentId(),
                            billingMonth,
                            billingYear
                    );
                });
    }

    // ==========================================================================
    // 12. Helper - Update Validation
    // ==========================================================================

    /**
     * Validate whether a bill can be updated.
     *
     * <p>Paid, partially paid, and cancelled bills are protected from editing
     * to preserve financial accuracy.
     *
     * @param billing billing entity
     */
    private void validateBillCanBeUpdated(Billing billing) {

        if (billing.getStatus() == BillingStatus.PAID
                || billing.getStatus() == BillingStatus.PARTIALLY_PAID
                || billing.getStatus() == BillingStatus.CANCELLED) {

            throw new InvalidBillingStatusException(
                    "Paid, partially paid, or cancelled bills cannot be updated."
            );
        }
    }

    // ==========================================================================
    // 13. Helper - Delete Validation
    // ==========================================================================

    /**
     * Validate whether a bill can be deleted.
     *
     * <p>Paid and partially paid bills must not be deleted because payment
     * records may already exist.
     *
     * @param billing billing entity
     */
    private void validateBillCanBeDeleted(Billing billing) {

        if (billing.getStatus() == BillingStatus.PAID
                || billing.getStatus() == BillingStatus.PARTIALLY_PAID) {

            throw new InvalidBillingStatusException(
                    "Paid or partially paid bills cannot be deleted."
            );
        }
    }

    // ==========================================================================
    // 14. Helper - Normalize Charge Fields
    // ==========================================================================

    /**
     * Replace nullable charge fields with zero.
     *
     * <p>This protects the total calculation when optional fields are sent as
     * explicit {@code null} in JSON.
     *
     * @param billing billing entity
     */
    private void normalizeChargeFields(Billing billing) {

        if (billing.getWaterCharge() == null) {
            billing.setWaterCharge(BigDecimal.ZERO);
        }

        if (billing.getElectricityCharge() == null) {
            billing.setElectricityCharge(BigDecimal.ZERO);
        }

        if (billing.getParkingCharge() == null) {
            billing.setParkingCharge(BigDecimal.ZERO);
        }

        if (billing.getPenalty() == null) {
            billing.setPenalty(BigDecimal.ZERO);
        }

        if (billing.getOtherCharge() == null) {
            billing.setOtherCharge(BigDecimal.ZERO);
        }
    }

    // ==========================================================================
    // 15. Helper - Total Amount Calculation
    // ==========================================================================

    /**
     * Calculate total payable amount.
     *
     * <p>Total amount is always calculated in backend. The frontend should not
     * be trusted to calculate or send the final amount.
     *
     * @param billing billing entity
     * @return total payable amount
     */
    private BigDecimal calculateTotalAmount(Billing billing) {

        return billing.getMaintenanceAmount()
                .add(billing.getWaterCharge())
                .add(billing.getElectricityCharge())
                .add(billing.getParkingCharge())
                .add(billing.getPenalty())
                .add(billing.getOtherCharge());
    }
}
