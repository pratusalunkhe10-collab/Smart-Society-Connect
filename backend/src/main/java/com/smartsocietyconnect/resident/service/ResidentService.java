package com.smartsocietyconnect.resident.service;

import java.util.List;

import com.smartsocietyconnect.resident.dto.request.CreateResidentRequest;
import com.smartsocietyconnect.resident.dto.request.UpdateResidentRequest;
import com.smartsocietyconnect.resident.dto.response.ResidentResponse;

/**
 * Service contract for resident management.
 *
 * Handles resident profile creation, lookup, update, flat-based queries,
 * and deletion/move-out operations.
 */
public interface ResidentService {

    /**
     * Creates a new resident profile and links it with a user and flat.
     *
     * @param request resident creation request
     * @return created resident response
     */
    ResidentResponse createResident(CreateResidentRequest request);

    /**
     * Finds a resident by resident ID.
     *
     * @param residentId resident primary key
     * @return matching resident response
     */
    ResidentResponse getResidentById(Integer residentId);

    /**
     * Finds a resident profile by linked user ID.
     *
     * @param userId linked user ID
     * @return matching resident response
     */
    ResidentResponse getResidentByUserId(Integer userId);

    /**
     * Returns all residents.
     *
     * @return list of residents
     */
    List<ResidentResponse> getAllResidents();

    /**
     * Returns all residents assigned to a flat.
     *
     * @param flatId flat ID
     * @return list of residents in the flat
     */
    List<ResidentResponse> getResidentsByFlat(Integer flatId);

    /**
     * Updates an existing resident profile.
     *
     * @param residentId resident primary key
     * @param request resident update request
     * @return updated resident response
     */
    ResidentResponse updateResident(Integer residentId, UpdateResidentRequest request);

    /**
     * Marks a resident as moved out or removes their flat association,
     * depending on your business logic.
     *
     * @param residentId resident primary key
     * @return updated resident response
     */
    ResidentResponse moveOutResident(Integer residentId);

    /**
     * Deletes a resident profile.
     *
     * @param residentId resident primary key
     */
    void deleteResident(Integer residentId);
}