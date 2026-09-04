package com.smartsocietyconnect.complaint.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.complaint.entity.Complaint;
import com.smartsocietyconnect.complaint.enums.ComplaintCategory;
import com.smartsocietyconnect.complaint.enums.ComplaintPriority;
import com.smartsocietyconnect.complaint.enums.ComplaintStatus;

/**
 * Repository interface for {@link Complaint} entity.
 *
 * <p>Provides CRUD and derived query operations for the {@code complaints} table.
 *
 * <p>Extends {@link JpaRepository} with:
 * <ul>
 *     <li>{@link Complaint} - entity type</li>
 *     <li>{@link Integer} - primary key type, matching {@code complaint_id}</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically creates query implementations from method
 * names, so no manual SQL is required for common complaint lookups.
 */
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Integer> {

    // ==========================================================================
    // Complaint Number Lookup
    // ==========================================================================

    /**
     * Finds a complaint by its public complaint number.
     *
     * <p>Use this when users search by tracking number such as
     * {@code CMP-20260701-0001}.
     *
     * @param complaintNumber unique complaint tracking number
     * @return matching complaint, if present
     */
    Optional<Complaint> findByComplaintNumber(String complaintNumber);

    /**
     * Checks whether a complaint number already exists.
     *
     * <p>Useful during complaint-number generation to avoid collisions.
     *
     * @param complaintNumber complaint tracking number
     * @return {@code true} if the number already exists
     */
    boolean existsByComplaintNumber(String complaintNumber);

    // ==========================================================================
    // Resident-Based Lookup
    // ==========================================================================

    /**
     * Finds all complaints raised by a resident.
     *
     * <p>Uses nested property query instead of passing a full Resident entity.
     *
     * @param residentId resident primary key
     * @return complaints raised by the resident
     */
    List<Complaint> findByResidentResidentId(Integer residentId);

    boolean existsByComplaintIdAndResidentUserEmailIgnoreCase(
            Integer complaintId,
            String email
    );

    /**
     * Finds complaints raised by a resident with a specific status.
     *
     * @param residentId resident primary key
     * @param status complaint status
     * @return matching complaints
     */
    List<Complaint> findByResidentResidentIdAndStatus(
            Integer residentId,
            ComplaintStatus status
    );

    // ==========================================================================
    // Status / Priority / Category Filters
    // ==========================================================================

    /**
     * Finds complaints by workflow status.
     *
     * @param status complaint status
     * @return complaints matching status
     */
    List<Complaint> findByStatus(ComplaintStatus status);

    /**
     * Finds complaints by priority.
     *
     * @param priority complaint priority
     * @return complaints matching priority
     */
    List<Complaint> findByPriority(ComplaintPriority priority);

    /**
     * Finds complaints by category.
     *
     * @param category complaint category
     * @return complaints matching category
     */
    List<Complaint> findByCategory(ComplaintCategory category);

    /**
     * Finds complaints by category and status.
     *
     * <p>Useful for dashboards such as all open plumbing complaints.
     *
     * @param category complaint category
     * @param status complaint status
     * @return matching complaints
     */
    List<Complaint> findByCategoryAndStatus(
            ComplaintCategory category,
            ComplaintStatus status
    );

    /**
     * Finds complaints by priority and status.
     *
     * <p>Useful for queues such as urgent open complaints.
     *
     * @param priority complaint priority
     * @param status complaint status
     * @return matching complaints
     */
    List<Complaint> findByPriorityAndStatus(
            ComplaintPriority priority,
            ComplaintStatus status
    );

    // ==========================================================================
    // Sorting
    // ==========================================================================

    /**
     * Returns all complaints ordered by latest first.
     *
     * @return complaints sorted by created timestamp descending
     */
    List<Complaint> findAllByOrderByCreatedAtDesc();
    
	 // ==========================================================================
	 // Dashboard Statistics
	 // ==========================================================================
	
	 long countByStatus(ComplaintStatus status);
	
	 long countByResidentResidentId(Integer residentId);
	
	 long countByResidentResidentIdAndStatus(
	         Integer residentId,
	         ComplaintStatus status
	 );
}
