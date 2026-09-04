package com.smartsocietyconnect.resident.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartsocietyconnect.common.payload.ApiResponse;
import com.smartsocietyconnect.resident.dto.request.AddFamilyMemberRequest;
import com.smartsocietyconnect.resident.dto.request.UpdateFamilyMemberRequest;
import com.smartsocietyconnect.resident.dto.response.FamilyMemberResponse;
import com.smartsocietyconnect.resident.service.FamilyMemberService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for family member management operations.
 *
 * <p>Exposes APIs for adding family members, fetching a single family member,
 * listing family members by resident, counting members, updating member details,
 * and deleting members.
 *
 * <p>This controller is intentionally thin. It validates request/path inputs,
 * maps HTTP endpoints to service calls, and delegates business rules to
 * {@link FamilyMemberService}.
 */
@Tag(
        name = "Family Member Management",
        description = "APIs for managing resident family member records"
)
@Validated
@RestController
@RequestMapping("/api/family-members")
@RequiredArgsConstructor
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;

    // ==========================================================================
    // 1. Add Family Member - POST /api/family-members
    // ==========================================================================

    /**
     * Adds a new family member under a resident profile.
     *
     * <p>The request body is validated using {@code @Valid}. The service layer
     * should verify that the resident exists and enforce any duplicate or
     * relationship-specific business rules.
     *
     * @param request family member creation request body
     * @return created family member wrapped in standard API response
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> addFamilyMember(
            @Valid @RequestBody AddFamilyMemberRequest request
    ) {
        FamilyMemberResponse response = familyMemberService.addFamilyMember(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(success("Family member added successfully", response));
    }

    // ==========================================================================
    // 2. Get Family Member By ID - GET /api/family-members/{memberId}
    // ==========================================================================

    /**
     * Fetches a family member by member ID.
     *
     * @param memberId family member primary key
     * @return family member details wrapped in standard API response
     */
    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> getFamilyMemberById(
            @Positive(message = "Member ID must be positive")
            @PathVariable Integer memberId
    ) {
        FamilyMemberResponse response = familyMemberService.getFamilyMemberById(memberId);

        return ResponseEntity.ok(
                success("Family member fetched successfully", response)
        );
    }

    // ==========================================================================
    // 3. Get Family Members By Resident - GET /api/family-members/resident/{residentId}
    // ==========================================================================

    /**
     * Fetches all family members linked to a resident.
     *
     * <p>The service layer should verify that the resident exists before
     * returning the member list.
     *
     * @param residentId resident primary key
     * @return list of family members for the resident
     */
    @GetMapping("/resident/{residentId}")
    public ResponseEntity<ApiResponse<List<FamilyMemberResponse>>> getFamilyMembersByResident(
            @Positive(message = "Resident ID must be positive")
            @PathVariable Integer residentId
    ) {
        List<FamilyMemberResponse> response =
                familyMemberService.getFamilyMembersByResident(residentId);

        return ResponseEntity.ok(
                success("Family members fetched successfully", response)
        );
    }

    // ==========================================================================
    // 4. Count Family Members - GET /api/family-members/resident/{residentId}/count
    // ==========================================================================

    /**
     * Counts family members linked to a resident.
     *
     * <p>This endpoint is useful for dashboards and lightweight summaries where
     * the client only needs the count, not the full member list.
     *
     * @param residentId resident primary key
     * @return count of family members for the resident
     */
    @GetMapping("/resident/{residentId}/count")
    public ResponseEntity<ApiResponse<Long>> countFamilyMembers(
            @Positive(message = "Resident ID must be positive")
            @PathVariable Integer residentId
    ) {
        Long count = familyMemberService.countFamilyMembersByResident(residentId);

        return ResponseEntity.ok(
                success("Family member count fetched successfully", count)
        );
    }

    // ==========================================================================
    // 5. Update Family Member - PUT /api/family-members/{memberId}
    // ==========================================================================

    /**
     * Updates an existing family member.
     *
     * <p>The update request is designed for partial updates. The service layer
     * should apply only non-null fields and keep existing values unchanged when
     * fields are not provided.
     *
     * @param memberId family member primary key
     * @param request family member update request body
     * @return updated family member wrapped in standard API response
     */
    @PutMapping("/{memberId}")
    public ResponseEntity<ApiResponse<FamilyMemberResponse>> updateFamilyMember(
            @Positive(message = "Member ID must be positive")
            @PathVariable Integer memberId,
            @Valid @RequestBody UpdateFamilyMemberRequest request
    ) {
        FamilyMemberResponse response =
                familyMemberService.updateFamilyMember(memberId, request);

        return ResponseEntity.ok(
                success("Family member updated successfully", response)
        );
    }

    // ==========================================================================
    // 6. Delete Family Member - DELETE /api/family-members/{memberId}
    // ==========================================================================

    /**
     * Deletes a family member by member ID.
     *
     * <p>The service layer should handle not-found checks and any business rules
     * related to deleting linked family member records.
     *
     * @param memberId family member primary key
     * @return success response with no data
     */
    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteFamilyMember(
            @Positive(message = "Member ID must be positive")
            @PathVariable Integer memberId
    ) {
        familyMemberService.deleteFamilyMember(memberId);

        return ResponseEntity.ok(
                success("Family member deleted successfully", null)
        );
    }

    // ==========================================================================
    // Private Helpers
    // ==========================================================================

    /**
     * Builds a standard successful API response.
     *
     * <p>Centralizing response creation keeps controller methods concise and
     * ensures all successful endpoints return the same response structure.
     *
     * @param message success message
     * @param data response payload
     * @param <T> response data type
     * @return standardized API response
     */
    private <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
}