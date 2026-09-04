package com.smartsocietyconnect.dashboard.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartsocietyconnect.billing.enums.BillingStatus;
import com.smartsocietyconnect.billing.repository.BillingRepository;
import com.smartsocietyconnect.billing.repository.PaymentHistoryRepository;
import com.smartsocietyconnect.complaint.enums.ComplaintStatus;
import com.smartsocietyconnect.complaint.repository.ComplaintRepository;
import com.smartsocietyconnect.dashboard.dto.AccountantDashboardResponse;
import com.smartsocietyconnect.dashboard.dto.AdminDashboardResponse;
import com.smartsocietyconnect.dashboard.dto.ResidentDashboardResponse;
import com.smartsocietyconnect.dashboard.dto.SecurityDashboardResponse;
import com.smartsocietyconnect.dashboard.service.DashboardService;
import com.smartsocietyconnect.resident.entity.FlatStatus;
import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.resident.repository.FlatRepository;
import com.smartsocietyconnect.resident.repository.ResidentRepository;
import com.smartsocietyconnect.visitor.enums.VisitorStatus;
import com.smartsocietyconnect.visitor.repository.VisitorRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service implementation for Dashboard module.
 *
 * <p>The dashboard module is read-only. It aggregates data from resident,
 * flat, visitor, complaint, billing, and payment modules.
 *
 * <p>Important:
 * Dashboard methods should use database-side count/sum queries instead of
 * loading full entity lists and calculating statistics in Java.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ResidentRepository residentRepository;
    private final FlatRepository flatRepository;
    private final VisitorRepository visitorRepository;
    private final ComplaintRepository complaintRepository;
    private final BillingRepository billingRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;

    // ==========================================================================
    // 1. Admin Dashboard
    // ==========================================================================

    /**
     * Returns society-wide statistics for admin users.
     *
     * @return admin dashboard response
     */
    @Override
    public AdminDashboardResponse getAdminDashboard() {

        DateRange dateRange = currentDateRange();

        return AdminDashboardResponse.builder()

                // Resident Statistics
                .totalResidents(residentRepository.count())
                .activeResidents(residentRepository.countByMoveOutDateIsNull())
                .inactiveResidents(residentRepository.countByMoveOutDateIsNotNull())

                // Flat Statistics
                .totalFlats(flatRepository.count())
                .occupiedFlats(flatRepository.countByStatus(FlatStatus.OCCUPIED))
                .vacantFlats(flatRepository.countByStatus(FlatStatus.VACANT))

                // Visitor Statistics
                .todayVisitors(visitorRepository.countByVisitDate(dateRange.today()))
                .pendingVisitors(visitorRepository.countByStatus(VisitorStatus.REQUESTED))
                .approvedVisitors(visitorRepository.countByStatus(VisitorStatus.APPROVED))
                .rejectedVisitors(visitorRepository.countByStatus(VisitorStatus.REJECTED))

                // Complaint Statistics
                .totalComplaints(complaintRepository.count())
                .openComplaints(complaintRepository.countByStatus(ComplaintStatus.OPEN))
                .inProgressComplaints(complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS))
                .resolvedComplaints(complaintRepository.countByStatus(ComplaintStatus.RESOLVED))

                // Billing Statistics
                .totalBills(billingRepository.count())
                .paidBills(billingRepository.countByStatus(BillingStatus.PAID))
                .pendingBills(billingRepository.countByStatus(BillingStatus.PENDING))

                // Revenue Statistics
                .totalCollection(paymentHistoryRepository.getTotalCollection())
                .pendingAmount(billingRepository.getOutstandingAmount())

                // Optional fields: add these to AdminDashboardResponse if you use them
                .todayCollection(paymentHistoryRepository.getCollectionBetween(
                        dateRange.todayStart(),
                        dateRange.tomorrowStart()
                ))
                .monthlyCollection(paymentHistoryRepository.getCollectionBetween(
                        dateRange.monthStart(),
                        dateRange.nextMonthStart()
                ))

                .build();
    }

    // ==========================================================================
    // 2. Resident Dashboard
    // ==========================================================================

    /**
     * Returns dashboard statistics for the authenticated resident.
     *
     * <p>The method accepts {@code userId}, not {@code residentId}, because
     * the logged-in user is identified through the authentication system.
     *
     * @param userId authenticated user's ID
     * @return resident dashboard response
     */
    @Override
    public ResidentDashboardResponse getResidentDashboard(Integer userId) {

        Resident resident = residentRepository.findByUserUserId(userId)
                .orElseThrow(() ->
                        new ResidentException(
                                "Resident profile not found for user ID: " + userId
                        )
                );

        Integer residentId = resident.getResidentId();
        LocalDate today = LocalDate.now();

        return ResidentDashboardResponse.builder()
                .residentId(residentId)
                .residentName(resolveResidentName(resident))
                .flatNumber(resolveFlatNumber(resident))

                .todayVisitors(
                        visitorRepository.countByResidentResidentIdAndVisitDate(
                                residentId,
                                today
                        )
                )
                .expectedVisitors(
                        visitorRepository.countByResidentResidentIdAndVisitDateAndStatus(
                                residentId,
                                today,
                                VisitorStatus.APPROVED
                        )
                )

                .myComplaints(
                        complaintRepository.countByResidentResidentId(residentId)
                )
                .openComplaints(
                        complaintRepository.countByResidentResidentIdAndStatus(
                                residentId,
                                ComplaintStatus.OPEN
                        )
                )

                .pendingBills(
                        billingRepository.countByResidentResidentIdAndStatus(
                                residentId,
                                BillingStatus.PENDING
                        )
                )
                .paidBills(
                        billingRepository.countByResidentResidentIdAndStatus(
                                residentId,
                                BillingStatus.PAID
                        )
                )
                .outstandingAmount(
                        billingRepository.getOutstandingAmountByResident(residentId)
                )
                .build();
    }

    // ==========================================================================
    // 3. Security Dashboard
    // ==========================================================================

    /**
     * Returns visitor monitoring statistics for security users.
     *
     * @return security dashboard response
     */
    @Override
    public SecurityDashboardResponse getSecurityDashboard() {

        LocalDate today = LocalDate.now();

        return SecurityDashboardResponse.builder()
                .todayVisitors(visitorRepository.countByVisitDate(today))
                .pendingVisitors(visitorRepository.countByStatus(VisitorStatus.REQUESTED))
                .approvedVisitors(visitorRepository.countByStatus(VisitorStatus.APPROVED))
                .rejectedVisitors(visitorRepository.countByStatus(VisitorStatus.REJECTED))
                .checkedInVisitors(visitorRepository.countByStatus(VisitorStatus.CHECKED_IN))
                .checkedOutVisitors(visitorRepository.countByStatus(VisitorStatus.CHECKED_OUT))
                .build();
    }

    // ==========================================================================
    // 4. Accountant Dashboard
    // ==========================================================================

    /**
     * Returns billing and payment statistics for accountant users.
     *
     * @return accountant dashboard response
     */
    @Override
    public AccountantDashboardResponse getAccountantDashboard() {

        DateRange dateRange = currentDateRange();

        return AccountantDashboardResponse.builder()
                .totalBills(billingRepository.count())
                .paidBills(billingRepository.countByStatus(BillingStatus.PAID))
                .pendingBills(billingRepository.countByStatus(BillingStatus.PENDING))
                .todayCollection(paymentHistoryRepository.getCollectionBetween(
                        dateRange.todayStart(),
                        dateRange.tomorrowStart()
                ))
                .monthlyCollection(paymentHistoryRepository.getCollectionBetween(
                        dateRange.monthStart(),
                        dateRange.nextMonthStart()
                ))
                .outstandingAmount(billingRepository.getOutstandingAmount())
                .build();
    }

    // ==========================================================================
    // 5. Helper Methods
    // ==========================================================================

    private DateRange currentDateRange() {

        LocalDate today = LocalDate.now();

        return new DateRange(
                today,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay(),
                today.withDayOfMonth(1).atStartOfDay(),
                today.plusMonths(1).withDayOfMonth(1).atStartOfDay()
        );
    }

    private String resolveResidentName(Resident resident) {

        if (resident.getUser() == null) {
            return null;
        }

        String firstName = resident.getUser().getFirstName() == null
                ? ""
                : resident.getUser().getFirstName();

        String lastName = resident.getUser().getLastName() == null
                ? ""
                : resident.getUser().getLastName();

        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? null : fullName;
    }

    private String resolveFlatNumber(Resident resident) {

        if (resident.getFlat() == null) {
            return null;
        }

        return resident.getFlat().getFlatNumber();
    }

    /**
     * Internal date range holder for dashboard collection queries.
     */
    private record DateRange(
            LocalDate today,
            LocalDateTime todayStart,
            LocalDateTime tomorrowStart,
            LocalDateTime monthStart,
            LocalDateTime nextMonthStart
    ) {
    }
}