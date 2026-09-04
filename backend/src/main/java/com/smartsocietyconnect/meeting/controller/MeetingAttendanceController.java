package com.smartsocietyconnect.meeting.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.smartsocietyconnect.meeting.dto.request.MeetingAttendanceRequest;
import com.smartsocietyconnect.meeting.dto.response.MeetingAttendanceResponse;
import com.smartsocietyconnect.meeting.service.MeetingAttendanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Meeting Attendance operations.
 *
 * <p>Provides APIs for submitting RSVP, updating RSVP, deleting RSVP,
 * viewing meeting attendees, viewing resident RSVP history and fetching
 * RSVP counts.
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Meeting Attendance",
        description = "APIs for managing meeting RSVP and attendance"
)
public class MeetingAttendanceController {

    private final MeetingAttendanceService meetingAttendanceService;

    // ==========================================================================
    // 1. Submit RSVP
    // ==========================================================================

    @PostMapping("/{meetingUuid}/attendance/{residentId}")
    @PreAuthorize("hasAuthority('RESIDENT')")
    @Operation(summary = "Submit meeting RSVP")
    public ResponseEntity<ApiResponse<MeetingAttendanceResponse>> submitAttendance(
            @PathVariable String meetingUuid,
            @PathVariable Integer residentId,
            @Valid @RequestBody MeetingAttendanceRequest request
    ) {

        MeetingAttendanceResponse response =
                meetingAttendanceService.submitAttendance(
                        meetingUuid,
                        residentId,
                        request
                );

        return ResponseEntity.ok(
                success(
                        "RSVP submitted successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 2. Update RSVP
    // ==========================================================================

    @PutMapping("/{meetingUuid}/attendance/{residentId}")
    @PreAuthorize("hasAuthority('RESIDENT')")
    @Operation(summary = "Update meeting RSVP")
    public ResponseEntity<ApiResponse<MeetingAttendanceResponse>> updateAttendance(
            @PathVariable String meetingUuid,
            @PathVariable Integer residentId,
            @Valid @RequestBody MeetingAttendanceRequest request
    ) {

        MeetingAttendanceResponse response =
                meetingAttendanceService.updateAttendance(
                        meetingUuid,
                        residentId,
                        request
                );

        return ResponseEntity.ok(
                success(
                        "RSVP updated successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 3. Delete RSVP
    // ==========================================================================

    @DeleteMapping("/{meetingUuid}/attendance/{residentId}")
    @PreAuthorize("hasAuthority('RESIDENT')")
    @Operation(summary = "Delete meeting RSVP")
    public ResponseEntity<ApiResponse<Void>> deleteAttendance(
            @PathVariable String meetingUuid,
            @PathVariable Integer residentId
    ) {

        meetingAttendanceService.deleteAttendance(
                meetingUuid,
                residentId
        );

        return ResponseEntity.ok(
                success(
                        "RSVP deleted successfully",
                        null
                )
        );
    }

    // ==========================================================================
    // 4. Get Meeting Attendees
    // ==========================================================================

    @GetMapping("/{meetingUuid}/attendance")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY', 'SECURITY')")
    @Operation(summary = "Get all meeting attendees")
    public ResponseEntity<ApiResponse<List<MeetingAttendanceResponse>>> getMeetingAttendees(
            @PathVariable String meetingUuid
    ) {

        List<MeetingAttendanceResponse> response =
                meetingAttendanceService.getMeetingAttendees(meetingUuid);

        return ResponseEntity.ok(
                success(
                        "Meeting attendees fetched successfully",
                        response
                )
        );
    }

    @GetMapping("/{meetingUuid}/attendance/resident/{residentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY', 'RESIDENT')")
    @Operation(summary = "Get one resident's RSVP for a meeting")
    public ResponseEntity<ApiResponse<MeetingAttendanceResponse>> getResidentAttendance(
            @PathVariable String meetingUuid,
            @PathVariable Integer residentId
    ) {
        return ResponseEntity.ok(success("Meeting RSVP fetched successfully",
                meetingAttendanceService.getResidentAttendance(meetingUuid, residentId)));
    }

    // ==========================================================================
    // 5. Get Resident RSVP History
    // ==========================================================================

    @GetMapping("/attendance/resident/{residentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY', 'RESIDENT')")
    @Operation(summary = "Get resident RSVP history")
    public ResponseEntity<ApiResponse<List<MeetingAttendanceResponse>>> getResidentAttendances(
            @PathVariable Integer residentId
    ) {

        List<MeetingAttendanceResponse> response =
                meetingAttendanceService.getResidentAttendances(residentId);

        return ResponseEntity.ok(
                success(
                        "Resident attendance history fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 6. Attendance Count - GOING
    // ==========================================================================

    @GetMapping("/{meetingUuid}/attendance/count/going")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY', 'SECURITY')")
    @Operation(summary = "Count GOING attendees")
    public ResponseEntity<ApiResponse<Long>> countGoing(
            @PathVariable String meetingUuid
    ) {

        Long response = meetingAttendanceService.countGoing(meetingUuid);

        return ResponseEntity.ok(
                success(
                        "Going count fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 7. Attendance Count - NOT_GOING
    // ==========================================================================

    @GetMapping("/{meetingUuid}/attendance/count/not-going")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY', 'SECURITY')")
    @Operation(summary = "Count NOT_GOING attendees")
    public ResponseEntity<ApiResponse<Long>> countNotGoing(
            @PathVariable String meetingUuid
    ) {

        Long response = meetingAttendanceService.countNotGoing(meetingUuid);

        return ResponseEntity.ok(
                success(
                        "Not going count fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 8. Attendance Count - MAYBE
    // ==========================================================================

    @GetMapping("/{meetingUuid}/attendance/count/maybe")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY', 'SECURITY')")
    @Operation(summary = "Count MAYBE attendees")
    public ResponseEntity<ApiResponse<Long>> countMaybe(
            @PathVariable String meetingUuid
    ) {

        Long response = meetingAttendanceService.countMaybe(meetingUuid);

        return ResponseEntity.ok(
                success(
                        "Maybe count fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 9. Attendance Count - PENDING
    // ==========================================================================

    @GetMapping("/{meetingUuid}/attendance/count/pending")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY', 'SECURITY')")
    @Operation(summary = "Count PENDING attendees")
    public ResponseEntity<ApiResponse<Long>> countPending(
            @PathVariable String meetingUuid
    ) {

        Long response = meetingAttendanceService.countPending(meetingUuid);

        return ResponseEntity.ok(
                success(
                        "Pending count fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 10. Response Helper
    // ==========================================================================

    /**
     * Builds a standard successful API response.
     */
    private <T> ApiResponse<T> success(String message, T data) {

        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
}
