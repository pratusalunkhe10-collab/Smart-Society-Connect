package com.smartsocietyconnect.resident.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a flat/unit in a Smart Society Connect society.
 *
 * <p>Maps to the {@code flats} table in the database.
 *
 * <p>Each flat has a type, occupancy status, wing, floor number, and area.
 * Residents are linked to flats through the {@code residents.flat_id} foreign key.
 *
 * <p><b>Important MySQL type mapping notes:</b>
 * <ul>
 *     <li>{@code flat_id} is {@code SMALLINT UNSIGNED}. Java uses {@code Integer}
 *         because unsigned SMALLINT supports values up to 65,535.</li>
 *     <li>{@code floor_number} is {@code TINYINT UNSIGNED}. Java uses {@code Short}
 *         because Java {@code Byte} is signed and cannot safely represent 0-255.</li>
 *     <li>{@code area_sqft} is {@code SMALLINT UNSIGNED}. Java uses {@code Integer}
 *         because unsigned SMALLINT can exceed Java {@code Short}'s max value.</li>
 * </ul>
 *
 * <p><b>Note on {@code FlatType} persistence:</b>
 * {@link FlatType} uses {@code @Convert} with {@link FlatType.FlatTypeConverter}
 * instead of {@code @Enumerated(EnumType.STRING)} because Java enum constants
 * may use names such as {@code _2BHK}, while the database stores clean values
 * such as {@code 2BHK}.
 *
 * <p><b>Lombok constructor strategy:</b>
 * <ul>
 *     <li>{@code @NoArgsConstructor} is required by JPA.</li>
 *     <li>{@code @AllArgsConstructor(access = AccessLevel.PRIVATE)} supports
 *         Lombok's builder while preventing direct all-args construction.</li>
 *     <li>{@code @Builder} provides a clean construction API.</li>
 * </ul>
 */
@Entity
@Table(name = "flats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Flat {

    /**
     * Primary key for the flat.
     *
     * <p>Maps to {@code flats.flat_id}, which is {@code SMALLINT UNSIGNED}.
     *
     * <p>{@code Integer} is used because MySQL unsigned SMALLINT supports
     * values from 0 to 65,535, while Java {@code Short} only supports up to 32,767.
     *
     * <p>{@code @JdbcTypeCode(SqlTypes.SMALLINT)} tells Hibernate to validate
     * this field against a SMALLINT database column instead of expecting INTEGER.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(
            name = "flat_id",
            columnDefinition = "SMALLINT UNSIGNED"
    )
    private Integer flatId;

    /**
     * Unique flat number.
     *
     * <p>Maps to {@code flats.flat_number}.
     * Examples: {@code A101}, {@code B-203}.
     */
    @Column(name = "flat_number", nullable = false, unique = true, length = 10)
    private String flatNumber;

    /**
     * Wing or block letter.
     *
     * <p>Maps to {@code flats.wing}, defined as {@code CHAR(1)}.
     * Examples: {@code A}, {@code B}, {@code C}.
     */
    @Column(name = "wing", nullable = false, columnDefinition = "CHAR(1)")
    private String wing;

    /**
     * Floor number of the flat.
     *
     * <p>Maps to {@code flats.floor_number}, which is {@code TINYINT UNSIGNED}.
     *
     * <p>{@code Short} safely supports the database range 0-255.
     */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(
            name = "floor_number",
            nullable = false,
            columnDefinition = "TINYINT UNSIGNED"
    )
    private Short floorNumber;

    /**
     * Flat configuration type.
     *
     * <p>Maps to the MySQL enum:
     * {@code ENUM('1RK','1BHK','2BHK','3BHK','4BHK')}.
     */
    @Convert(converter = FlatType.FlatTypeConverter.class)
    @Column(
            name = "flat_type",
            nullable = false,
            columnDefinition = "ENUM('1RK','1BHK','2BHK','3BHK','4BHK')"
    )
    private FlatType flatType;

    /**
     * Current occupancy status.
     *
     * <p>Maps to {@code flats.status}.
     *
     * <p>{@code @Builder.Default} is important because Lombok builder otherwise
     * ignores field initializers and would set this field to {@code null}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            columnDefinition = "ENUM('VACANT','OCCUPIED') DEFAULT 'VACANT'"
    )
    private FlatStatus status = FlatStatus.VACANT;

    /**
     * Area of the flat in square feet.
     *
     * <p>Maps to {@code flats.area_sqft}, which is {@code SMALLINT UNSIGNED}.
     *
     * <p>This mapping fixes the Hibernate validation error:
     * {@code found smallint unsigned, but expecting integer}.
     */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(
            name = "area_sqft",
            columnDefinition = "SMALLINT UNSIGNED"
    )
    private Integer areaSqft;

    /**
     * Timestamp when this flat was created.
     *
     * <p>Automatically populated by Hibernate during insert.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}