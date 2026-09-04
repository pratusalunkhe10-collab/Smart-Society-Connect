package com.smartsocietyconnect.visitor.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.visitor.enums.Gender;
import com.smartsocietyconnect.visitor.enums.IdProofType;
import com.smartsocietyconnect.visitor.enums.VisitorStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a visitor record in Smart Society Connect.
 *
 * <p>Maps to the {@code visitors} table in the database.
 *
 * <p>A visitor record stores visitor personal details, identity proof details,
 * visit timing information, approval information, and check-in/check-out status.
 *
 * <p><b>Relationships:</b>
 * <ul>
 *     <li>{@code resident_id} links the visitor to the resident being visited.</li>
 *     <li>{@code created_by} links to the user who created the visitor entry.</li>
 *     <li>{@code approved_by} links to the user who approved or rejected the request.</li>
 * </ul>
 *
 * <p><b>Lifecycle:</b>
 * <pre>
 * REQUESTED -> APPROVED / REJECTED
 * APPROVED  -> CHECKED_IN
 * CHECKED_IN -> CHECKED_OUT
 * </pre>
 *
 * <p><b>Lombok constructor strategy:</b>
 * <ul>
 *     <li>{@code @NoArgsConstructor} is required by JPA.</li>
 *     <li>{@code @AllArgsConstructor(access = AccessLevel.PRIVATE)} supports
 *         Lombok builder while preventing direct all-args construction.</li>
 *     <li>{@code @Builder} provides clean object construction.</li>
 * </ul>
 */
@Entity
@Table(
        name = "visitors",
        indexes = {
                @Index(name = "idx_visitor_mobile", columnList = "mobile"),
                @Index(name = "idx_visitor_resident", columnList = "resident_id"),
                @Index(name = "idx_visitor_status", columnList = "status"),
                @Index(name = "idx_visitor_visit_date", columnList = "visit_date"),
                @Index(name = "idx_visitor_checkin", columnList = "check_in_time")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Visitor {

    // ==========================================================================
    // Primary Key
    // ==========================================================================

    /**
     * Primary key for the visitor record.
     *
     * <p>Maps to {@code visitors.visitor_id}, which is {@code INT UNSIGNED}.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visitor_id", columnDefinition = "INT UNSIGNED")
    private Integer visitorId;

    // ==========================================================================
    // Relationships
    // ==========================================================================

    /**
     * Resident whom the visitor is visiting.
     *
     * <p>Maps to {@code visitors.resident_id}. This column is NOT NULL and has
     * {@code ON DELETE CASCADE} at the database level.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    /**
     * User who approved or rejected this visitor request.
     *
     * <p>Maps to {@code visitors.approved_by}. Nullable until approval/rejection.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    /**
     * User who created the visitor entry.
     *
     * <p>Maps to {@code visitors.created_by}. This field is required.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // ==========================================================================
    // Visitor Personal Details
    // ==========================================================================

    /**
     * Full name of the visitor.
     */
    @NotBlank(message = "Visitor name is required")
    @Size(max = 100, message = "Visitor name must not exceed 100 characters")
    @Column(name = "visitor_name", nullable = false, length = 100)
    private String visitorName;

    /**
     * Visitor mobile number.
     *
     * <p>Maps to {@code visitors.mobile}, defined as {@code CHAR(10)}.
     */
    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Invalid mobile number"
    )
    @Column(name = "mobile", nullable = false, columnDefinition = "CHAR(10)")
    private String mobile;

    /**
     * Visitor gender.
     *
     * <p>Maps to MySQL ENUM('MALE','FEMALE','OTHER').
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "gender",
            nullable = false,
            columnDefinition = "ENUM('MALE','FEMALE','OTHER')"
    )
    private Gender gender;

    /**
     * Visitor age.
     *
     * <p>Maps to {@code visitors.age}, which is {@code TINYINT UNSIGNED}.
     * Java uses {@code Short} because TINYINT UNSIGNED supports 0-255.
     */
    @Min(value = 1, message = "Age must be at least 1")
    @Max(value = 120, message = "Age must not exceed 120")
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "age", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private Short age;

    /**
     * Visitor address.
     */
    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Column(name = "address", nullable = false, length = 255)
    private String address;

    // ==========================================================================
    // Identity Proof Details
    // ==========================================================================

    /**
     * Type of identity proof provided by the visitor.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "id_proof_type",
            nullable = false,
            columnDefinition = "ENUM('AADHAAR','PAN','PASSPORT','DRIVING_LICENSE','VOTER_ID','OTHER')"
    )
    private IdProofType idProofType;

    /**
     * Identity proof number.
     */
    @NotBlank(message = "ID proof number is required")
    @Size(max = 30, message = "ID proof number must not exceed 30 characters")
    @Column(name = "id_proof_number", nullable = false, length = 30)
    private String idProofNumber;

    /**
     * Optional photo URL/path of the visitor.
     */
    @Size(max = 255, message = "Photo URL must not exceed 255 characters")
    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    /**
     * Optional visitor vehicle number.
     */
    @Size(max = 20, message = "Vehicle number must not exceed 20 characters")
    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    // ==========================================================================
    // Visit Details
    // ==========================================================================

    /**
     * Purpose of the visit.
     */
    @NotBlank(message = "Purpose is required")
    @Size(max = 255, message = "Purpose must not exceed 255 characters")
    @Column(name = "purpose", nullable = false, length = 255)
    private String purpose;

    /**
     * Date of visit.
     */
    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    /**
     * Expected visit time.
     */
    @Column(name = "expected_time")
    private LocalTime expectedTime;

    /**
     * Actual check-in date and time.
     */
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    /**
     * Actual check-out date and time.
     */
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    // ==========================================================================
    // Workflow Status
    // ==========================================================================

    /**
     * Current visitor request status.
     *
     * <p>Defaults to {@link VisitorStatus#REQUESTED}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            columnDefinition = "ENUM('REQUESTED','APPROVED','REJECTED','CHECKED_IN','CHECKED_OUT') DEFAULT 'REQUESTED'"
    )
    private VisitorStatus status = VisitorStatus.REQUESTED;

    /**
     * Optional remarks added during approval, rejection, or security processing.
     */
    @Size(max = 255, message = "Remarks must not exceed 255 characters")
    @Column(name = "remarks", length = 255)
    private String remarks;

    // ==========================================================================
    // Audit Fields
    // ==========================================================================

    /**
     * Timestamp when visitor record was created.
     *
     * <p>Automatically populated by Hibernate on insert.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when visitor record was last updated.
     *
     * <p>Automatically updated by Hibernate on every update.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}