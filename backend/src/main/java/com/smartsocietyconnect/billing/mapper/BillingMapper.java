package com.smartsocietyconnect.billing.mapper;

import org.springframework.stereotype.Component;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.billing.dto.BillingRequest;
import com.smartsocietyconnect.billing.dto.BillingResponse;
import com.smartsocietyconnect.billing.entity.Billing;
import com.smartsocietyconnect.resident.entity.Flat;
import com.smartsocietyconnect.resident.entity.Resident;

/**
 * Mapper class for converting between Billing DTOs and Billing entity.
 *
 * <p>This project uses manual mappers instead of MapStruct to keep the mapping
 * style consistent with the Resident, Visitor, and Complaint modules.
 *
 * <p><b>Mapper responsibility:</b>
 * <ul>
 *     <li>Convert request DTO into entity.</li>
 *     <li>Convert entity into response DTO.</li>
 *     <li>Keep controller and service code cleaner.</li>
 * </ul>
 *
 * <p><b>Important:</b>
 * Business rules should not be placed inside mapper classes.
 * For example, duplicate bill validation, total amount calculation, and payment
 * status updates belong in the service layer.
 */
@Component
public class BillingMapper {

    // ==========================================================================
    // 1. Convert Request DTO to Entity
    // ==========================================================================

    /**
     * Convert {@link BillingRequest} to {@link Billing}.
     *
     * <p>The {@link Resident} is passed from the service layer because it must
     * be fetched from the database. The client sends only {@code residentId}.
     *
     * <p>The total amount is intentionally not set here. It should be calculated
     * in the service layer before saving the bill.
     *
     * @param request billing request from client
     * @param resident resident entity fetched by service layer
     * @return billing entity
     */
    public Billing toEntity(BillingRequest request, Resident resident) {

        if (request == null) {
            return null;
        }

        return Billing.builder()
                .resident(resident)
                .billingMonth(request.getBillingMonth())
                .billingYear(request.getBillingYear())
                .maintenanceAmount(request.getMaintenanceAmount())
                .waterCharge(request.getWaterCharge())
                .electricityCharge(request.getElectricityCharge())
                .parkingCharge(request.getParkingCharge())
                .penalty(request.getPenalty())
                .otherCharge(request.getOtherCharge())
                .dueDate(request.getDueDate())
                .remarks(request.getRemarks())
                .build();
    }

    // ==========================================================================
    // 2. Convert Entity to Response DTO
    // ==========================================================================

    /**
     * Convert {@link Billing} entity to {@link BillingResponse}.
     *
     * <p>This method flattens nested entity data such as resident name and flat
     * number into a client-friendly response.
     *
     * @param billing billing entity
     * @return billing response DTO
     */
    public BillingResponse toResponse(Billing billing) {

        if (billing == null) {
            return null;
        }

        Resident resident = billing.getResident();

        return BillingResponse.builder()
                .billingId(billing.getBillingId())
                .residentId(resolveResidentId(resident))
                .residentName(resolveResidentName(resident))
                .flatNumber(resolveFlatNumber(resident))
                .billingMonth(billing.getBillingMonth())
                .billingYear(billing.getBillingYear())
                .maintenanceAmount(billing.getMaintenanceAmount())
                .waterCharge(billing.getWaterCharge())
                .electricityCharge(billing.getElectricityCharge())
                .parkingCharge(billing.getParkingCharge())
                .penalty(billing.getPenalty())
                .otherCharge(billing.getOtherCharge())
                .totalAmount(billing.getTotalAmount())
                .dueDate(billing.getDueDate())
                .status(billing.getStatus())
                .remarks(billing.getRemarks())
                .createdAt(billing.getCreatedAt())
                .updatedAt(billing.getUpdatedAt())
                .build();
    }

    // ==========================================================================
    // 3. Private Helper Methods
    // ==========================================================================

    /**
     * Resolve resident ID safely.
     */
    private Integer resolveResidentId(Resident resident) {

        if (resident == null) {
            return null;
        }

        return resident.getResidentId();
    }

    /**
     * Resolve resident full name safely.
     */
    private String resolveResidentName(Resident resident) {

        if (resident == null || resident.getUser() == null) {
            return null;
        }

        User user = resident.getUser();

        String firstName = user.getFirstName() == null
                ? ""
                : user.getFirstName();

        String lastName = user.getLastName() == null
                ? ""
                : user.getLastName();

        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? null : fullName;
    }

    /**
     * Resolve resident flat number safely.
     */
    private String resolveFlatNumber(Resident resident) {

        if (resident == null || resident.getFlat() == null) {
            return null;
        }

        Flat flat = resident.getFlat();

        return flat.getFlatNumber();
    }
}