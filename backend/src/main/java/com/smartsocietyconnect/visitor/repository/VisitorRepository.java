package com.smartsocietyconnect.visitor.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smartsocietyconnect.visitor.entity.Visitor;
import com.smartsocietyconnect.visitor.enums.VisitorStatus;

/**
 * Repository interface for {@link Visitor} entity.
 *
 * <p>Provides CRUD and derived query operations for the {@code visitors} table.
 *
 * <p>Extends {@link JpaRepository} with:
 * <ul>
 *     <li>{@link Visitor} - entity type</li>
 *     <li>{@link Integer} - primary key type, matching {@code visitor_id}</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically implements these methods based on method
 * names, so no manual SQL is required for common lookup operations.
 */
@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Integer> {

    // ==========================================================================
    // Identity Proof Lookup
    // ==========================================================================

    /**
     * Finds a visitor by identity proof number.
     *
     * <p>Useful when checking previous visit history for the same person.
     *
     * @param idProofNumber ID proof number
     * @return visitor record if found
     */
    Optional<Visitor> findByIdProofNumber(String idProofNumber);

    /**
     * Checks whether a visitor with the given ID proof number already exists.
     *
     * <p>Use this for duplicate visitor validation if your business rule allows
     * only one active visitor record per ID proof.
     *
     * @param idProofNumber ID proof number
     * @return {@code true} if matching visitor exists
     */
    boolean existsByIdProofNumber(String idProofNumber);

    // ==========================================================================
    // Resident-Based Lookup
    // ==========================================================================

    /**
     * Finds all visitors linked to a resident.
     *
     * @param residentId resident primary key
     * @return list of visitors for the resident
     */
    List<Visitor> findByResidentResidentId(Integer residentId);

    boolean existsByVisitorIdAndResidentUserEmailIgnoreCase(
            Integer visitorId,
            String email
    );

    /**
     * Finds all visitors for a resident with a specific status.
     *
     * @param residentId resident primary key
     * @param status visitor status
     * @return list of matching visitors
     */
    List<Visitor> findByResidentResidentIdAndStatus(
            Integer residentId,
            VisitorStatus status
    );

    // ==========================================================================
    // Status And Date Lookup
    // ==========================================================================

    /**
     * Finds all visitors by workflow status.
     *
     * @param status visitor status
     * @return list of visitors with given status
     */
    List<Visitor> findByStatus(VisitorStatus status);

    /**
     * Finds all visitors scheduled for a visit date.
     *
     * @param visitDate visit date
     * @return list of visitors for the date
     */
    List<Visitor> findByVisitDate(LocalDate visitDate);

    /**
     * Finds all visitors scheduled for a visit date with a specific status.
     *
     * @param visitDate visit date
     * @param status visitor status
     * @return list of matching visitors
     */
    List<Visitor> findByVisitDateAndStatus(
            LocalDate visitDate,
            VisitorStatus status
    );

    // ==========================================================================
    // Mobile Lookup
    // ==========================================================================

    /**
     * Finds all visitors by mobile number.
     *
     * <p>A mobile number may appear in multiple visitor records over time,
     * so this method returns a list instead of a single optional record.
     *
     * @param mobile mobile number
     * @return list of visitors with this mobile number
     */
    List<Visitor> findByMobile(String mobile);
    
	 // ==========================================================================
	 // Dashboard Statistics
	 // ==========================================================================
	
	 long countByVisitDate(LocalDate visitDate);
	
	 long countByStatus(VisitorStatus status);
	
	 long countByVisitDateAndStatus(
	         LocalDate visitDate,
	         VisitorStatus status
	 );
	
	 long countByResidentResidentId(Integer residentId);
	 long countByResidentResidentIdAndVisitDate(
		        Integer residentId,
		        LocalDate visitDate
		);

		long countByResidentResidentIdAndVisitDateAndStatus(
		        Integer residentId,
		        LocalDate visitDate,
		        VisitorStatus status
		);
}
