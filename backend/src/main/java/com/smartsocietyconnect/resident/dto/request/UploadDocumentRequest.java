package com.smartsocietyconnect.resident.dto.request;

import com.smartsocietyconnect.resident.entity.DocumentType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for uploading a document for a resident.
 *
 * <p>This DTO is sent as the {@code request} part in a multipart/form-data
 * request. The actual file is sent separately as the {@code file} part.
 *
 * <p>Important: {@code documentType} means the category of document, not the
 * file format. For example, use {@code AADHAAR}, {@code PAN}, or
 * {@code RENT_AGREEMENT}. Do not use {@code PDF}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Document upload metadata")
public class UploadDocumentRequest {

    /**
     * ID of the resident this document belongs to.
     */
    @NotNull(message = "Resident ID is required")
    @Schema(
            description = "Resident ID of the document owner",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer residentId;

    /**
     * Category/type of the document.
     *
     * <p>Allowed values depend on your {@link DocumentType} enum.
     */
    @NotNull(message = "Document type is required")
    @Schema(
            description = "Document category, not file format",
            example = "AADHAAR",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private DocumentType documentType;

    /**
     * Display name of the uploaded document.
     */
    @NotBlank(message = "Document name is required")
    @Size(max = 100, message = "Document name cannot exceed 100 characters")
    @Schema(
            description = "Display name of the document",
            example = "AADHAAR CARD",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String documentName;
}