package com.smartsocietyconnect.meeting.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartsocietyconnect.common.payload.ApiResponse;
import com.smartsocietyconnect.meeting.dto.request.CreateMeetingRequest;
import com.smartsocietyconnect.meeting.dto.request.MeetingMinutesRequest;
import com.smartsocietyconnect.meeting.dto.request.UpdateMeetingRequest;
import com.smartsocietyconnect.meeting.dto.response.MeetingDetailsResponse;
import com.smartsocietyconnect.meeting.dto.response.MeetingMinutesResponse;
import com.smartsocietyconnect.meeting.dto.response.MeetingResponse;
import com.smartsocietyconnect.meeting.enums.MeetingStatus;
import com.smartsocietyconnect.meeting.service.MeetingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Meeting Module.
 *
 * <p>Provides APIs for meeting scheduling, meeting updates, meeting status
 * changes, meeting cancellation, meeting search and Minutes of Meeting.
 */
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Meeting Management",
        description = "APIs for managing society meetings"
)
public class MeetingController {

    private final MeetingService meetingService;

    // ==========================================================================
    // 1. Create Meeting
    // ==========================================================================

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY')")
    @Operation(summary = "Create a new meeting")
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
            @Valid @RequestBody CreateMeetingRequest request
    ) {

        MeetingResponse response = meetingService.createMeeting(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(success(
                        "Meeting created successfully",
                        response
                ));
    }

    // ==========================================================================
    // 2. Update Meeting
    // ==========================================================================

    @PutMapping("/{meetingUuid}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY')")
    @Operation(summary = "Update meeting")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeeting(
            @PathVariable String meetingUuid,
            @Valid @RequestBody UpdateMeetingRequest request
    ) {

        MeetingResponse response = meetingService.updateMeeting(
                meetingUuid,
                request
        );

        return ResponseEntity.ok(
                success(
                        "Meeting updated successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 3. Update Meeting Status
    // ==========================================================================

    @PatchMapping("/{meetingUuid}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY')")
    @Operation(summary = "Update meeting status")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeetingStatus(
            @PathVariable String meetingUuid,
            @RequestParam MeetingStatus status
    ) {

        MeetingResponse response = meetingService.updateMeetingStatus(
                meetingUuid,
                status
        );

        return ResponseEntity.ok(
                success(
                        "Meeting status updated successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 4. Cancel Meeting
    // ==========================================================================

    @PatchMapping("/{meetingUuid}/cancel")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY')")
    @Operation(summary = "Cancel meeting")
    public ResponseEntity<ApiResponse<MeetingResponse>> cancelMeeting(
            @PathVariable String meetingUuid
    ) {

        MeetingResponse response = meetingService.cancelMeeting(meetingUuid);

        return ResponseEntity.ok(
                success(
                        "Meeting cancelled successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 5. Delete Meeting
    // ==========================================================================

    @DeleteMapping("/{meetingUuid}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete meeting")
    public ResponseEntity<ApiResponse<Void>> deleteMeeting(
            @PathVariable String meetingUuid
    ) {

        meetingService.deleteMeeting(meetingUuid);

        return ResponseEntity.ok(
                success(
                        "Meeting deleted successfully",
                        null
                )
        );
    }

    // ==========================================================================
    // 6. Get All Meetings
    // ==========================================================================

    @GetMapping
    @Operation(summary = "Get all meetings")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getAllMeetings() {

        List<MeetingResponse> response = meetingService.getAllMeetings();

        return ResponseEntity.ok(
                success(
                        "Meetings fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 7. Get Upcoming Meetings
    // ==========================================================================

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming meetings")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getUpcomingMeetings() {

        List<MeetingResponse> response = meetingService.getUpcomingMeetings();

        return ResponseEntity.ok(
                success(
                        "Upcoming meetings fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 8. Get Completed Meetings
    // ==========================================================================

    @GetMapping("/completed")
    @Operation(summary = "Get completed meetings")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getCompletedMeetings() {

        List<MeetingResponse> response = meetingService.getCompletedMeetings();

        return ResponseEntity.ok(
                success(
                        "Completed meetings fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 9. Get Meetings By Status
    // ==========================================================================

    @GetMapping("/status/{status}")
    @Operation(summary = "Get meetings by status")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMeetingsByStatus(
            @PathVariable MeetingStatus status
    ) {

        List<MeetingResponse> response =
                meetingService.getMeetingsByStatus(status);

        return ResponseEntity.ok(
                success(
                        "Meetings fetched by status successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 10. Search Meetings
    // ==========================================================================

    @GetMapping("/search")
    @Operation(summary = "Search meetings by title")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> searchMeetings(
            @RequestParam String keyword
    ) {

        List<MeetingResponse> response =
                meetingService.searchMeetings(keyword);

        return ResponseEntity.ok(
                success(
                        "Meetings searched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 11. Get Meeting Details
    // ==========================================================================

    @GetMapping("/{meetingUuid}")
    @Operation(summary = "Get meeting details")
    public ResponseEntity<ApiResponse<MeetingDetailsResponse>> getMeeting(
            @PathVariable String meetingUuid
    ) {

        MeetingDetailsResponse response =
                meetingService.getMeetingByUuid(meetingUuid);

        return ResponseEntity.ok(
                success(
                        "Meeting details fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 12. Create Or Update Meeting Minutes
    // ==========================================================================

    @PostMapping("/{meetingUuid}/minutes")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY')")
    @Operation(summary = "Create or update meeting minutes")
    public ResponseEntity<ApiResponse<MeetingMinutesResponse>> uploadMeetingMinutes(
            @PathVariable String meetingUuid,
            @Valid @RequestBody MeetingMinutesRequest request
    ) {

        MeetingMinutesResponse response =
                meetingService.uploadMeetingMinutes(
                        meetingUuid,
                        request
                );

        return ResponseEntity.ok(
                success(
                        "Meeting minutes saved successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 13. Get Meeting Minutes
    // ==========================================================================

    @GetMapping("/{meetingUuid}/minutes")
    @Operation(summary = "Get meeting minutes")
    public ResponseEntity<ApiResponse<MeetingMinutesResponse>> getMeetingMinutes(
            @PathVariable String meetingUuid
    ) {

        MeetingMinutesResponse response =
                meetingService.getMeetingMinutes(meetingUuid);

        return ResponseEntity.ok(
                success(
                        "Meeting minutes fetched successfully",
                        response
                )
        );
    }

    // ==========================================================================
    // 14. Response Helper
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