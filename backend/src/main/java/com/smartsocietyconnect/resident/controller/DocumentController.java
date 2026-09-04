package com.smartsocietyconnect.resident.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import com.smartsocietyconnect.common.payload.ApiResponse;
import com.smartsocietyconnect.resident.dto.request.UploadDocumentRequest;
import com.smartsocietyconnect.resident.dto.response.DocumentResponse;
import com.smartsocietyconnect.resident.entity.DocumentType;
import com.smartsocietyconnect.resident.entity.VerificationStatus;
import com.smartsocietyconnect.resident.service.DocumentService;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for resident document management operations.
 *
 * <p>Exposes APIs for uploading resident documents, fetching documents,
 * filtering documents by verification status, verifying documents,
 * rejecting documents, and deleting documents.
 *
 * <p>This controller is intentionally thin. It handles HTTP request mapping,
 * request parameter validation, and response wrapping. Business rules such as
 * file validation, file storage, document metadata persistence, and verification
 * workflow are delegated to {@link DocumentService}.
 *
 * <p><b>Swagger multipart upload note:</b>
 * Instead of accepting a nested JSON object inside multipart form-data, this
 * controller accepts simple multipart form fields:
 * <ul>
 *     <li>{@code residentId}</li>
 *     <li>{@code documentType}</li>
 *     <li>{@code documentName}</li>
 *     <li>{@code file}</li>
 * </ul>
 *
 * <p>This works better with Swagger UI because it generates clean multipart
 * requests without forcing users to manually format JSON inside a form field.
 */
@Tag(
        name = "Resident Document Management",
        description = "APIs for uploading, verifying, rejecting, and deleting resident documents"
)
@Validated
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // ==========================================================================
    // 1. Upload Document - POST /api/documents/upload
    // ==========================================================================

    /**
     * Uploads a document for a resident.
     *
     * <p>Expected multipart form-data fields:
     * <ul>
     *     <li>{@code residentId}: ID of the resident who owns the document</li>
     *     <li>{@code documentType}: category of document, e.g. AADHAAR, PAN</li>
     *     <li>{@code documentName}: display name of the document</li>
     *     <li>{@code file}: uploaded file binary</li>
     * </ul>
     *
     * <p>Important: {@code documentType} means the document category, not the
     * file format. Use {@code AADHAAR}, {@code PAN}, {@code RENT_AGREEMENT},
     * etc. Do not use {@code PDF}; PDF is detected from the uploaded file's
     * MIME type.
     *
     * @param residentId resident ID that owns this document
     * @param documentType document category
     * @param documentName display name of the uploaded document
     * @param file uploaded file
     * @return uploaded document details wrapped in standard API response
     */
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY') or @ownershipAuthorization.isCurrentResident(#residentId, authentication)")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @NotNull(message = "Resident ID is required")
            @Positive(message = "Resident ID must be positive")
            @RequestParam Integer residentId,

            @NotNull(message = "Document type is required")
            @RequestParam DocumentType documentType,

            @NotBlank(message = "Document name is required")
            @Size(max = 100, message = "Document name cannot exceed 100 characters")
            @RequestParam String documentName,

            @RequestParam("file") MultipartFile file
    ) {
        UploadDocumentRequest request = UploadDocumentRequest.builder()
                .residentId(residentId)
                .documentType(documentType)
                .documentName(documentName)
                .build();

        DocumentResponse response = documentService.uploadDocument(request, file);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(success("Document uploaded successfully", response));
    }

    // ==========================================================================
    // 2. Get Document By ID - GET /api/documents/{residentDocId}
    // ==========================================================================

    /**
     * Fetches a document by resident-document mapping ID.
     *
     * <p>The ID refers to {@code resident_documents.resident_doc_id}, not the
     * raw {@code documents.document_id}.
     *
     * @param residentDocId resident-document mapping primary key
     * @return document details wrapped in standard API response
     */
    @GetMapping("/{residentDocId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY') or @ownershipAuthorization.ownsDocument(#residentDocId, authentication)")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentById(
            @Positive(message = "Document ID must be positive")
            @PathVariable Integer residentDocId
    ) {
        DocumentResponse response = documentService.getDocumentById(residentDocId);

        return ResponseEntity.ok(
                success("Document fetched successfully", response)
        );
    }

    /** Returns an uploaded PDF or image for an authorized reviewer or its owner. */
    @GetMapping("/{residentDocId}/file")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY') or @ownershipAuthorization.ownsDocument(#residentDocId, authentication)")
    public ResponseEntity<Resource> viewDocumentFile(
            @Positive(message = "Document ID must be positive")
            @PathVariable Integer residentDocId
    ) {
        DocumentResponse document = documentService.getDocumentById(residentDocId);
        MediaType mediaType;
        try {
            mediaType = document.getMimeType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(document.getMimeType());
        } catch (IllegalArgumentException ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(document.getDocumentName())
                        .build().toString())
                .body(documentService.loadDocumentFile(residentDocId));
    }

    // ==========================================================================
    // 3. Get Documents By Resident - GET /api/documents/resident/{residentId}
    // ==========================================================================

    /**
     * Fetches all documents uploaded for a specific resident.
     *
     * <p>This may include documents across all verification states:
     * {@code PENDING}, {@code VERIFIED}, and {@code REJECTED}.
     *
     * @param residentId resident primary key
     * @return list of documents belonging to the resident
     */
    @GetMapping("/resident/{residentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY') or @ownershipAuthorization.isCurrentResident(#residentId, authentication)")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByResident(
            @Positive(message = "Resident ID must be positive")
            @PathVariable Integer residentId
    ) {
        List<DocumentResponse> response =
                documentService.getDocumentsByResident(residentId);

        return ResponseEntity.ok(
                success("Documents fetched successfully", response)
        );
    }

    // ==========================================================================
    // 4. Get Documents By Status - GET /api/documents/status/{status}
    // ==========================================================================

    /**
     * Fetches documents by verification status.
     *
     * <p>Useful for admin queues, especially listing all
     * {@link VerificationStatus#PENDING} documents awaiting review.
     *
     * @param status verification status filter
     * @return list of documents matching the status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByStatus(
            @PathVariable VerificationStatus status
    ) {
        List<DocumentResponse> response =
                documentService.getDocumentsByStatus(status);

        return ResponseEntity.ok(
                success("Documents fetched successfully", response)
        );
    }

    /** Staff register used to search documents across all residents and statuses. */
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getAllDocuments() {
        return ResponseEntity.ok(success(
                "Documents fetched successfully", documentService.getAllDocuments()));
    }

    // ==========================================================================
    // 5. Verify Document - PATCH /api/documents/{residentDocId}/verify
    // ==========================================================================

    /**
     * Marks a pending document as verified.
     *
     * <p>The verifier user should normally be an admin/staff user. Role checks
     * should be enforced through Spring Security.
     *
     * @param residentDocId resident-document mapping primary key
     * @param verifierUserId user ID of the verifier
     * @return verified document details
     */
    @PatchMapping("/{residentDocId}/verify")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY')")
    public ResponseEntity<ApiResponse<DocumentResponse>> verifyDocument(
            @Positive(message = "Document ID must be positive")
            @PathVariable Integer residentDocId,

            @Positive(message = "Verifier user ID must be positive")
            @RequestParam Integer verifierUserId
    ) {
        DocumentResponse response =
                documentService.verifyDocument(residentDocId, verifierUserId);

        return ResponseEntity.ok(
                success("Document verified successfully", response)
        );
    }

    // ==========================================================================
    // 6. Reject Document - PATCH /api/documents/{residentDocId}/reject
    // ==========================================================================

    /**
     * Marks a pending document as rejected.
     *
     * <p>The rejected document record is kept for audit history. The resident
     * can upload a corrected document separately.
     *
     * @param residentDocId resident-document mapping primary key
     * @param verifierUserId user ID of the verifier/reviewer
     * @return rejected document details
     */
    @PatchMapping("/{residentDocId}/reject")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY')")
    public ResponseEntity<ApiResponse<DocumentResponse>> rejectDocument(
            @Positive(message = "Document ID must be positive")
            @PathVariable Integer residentDocId,

            @Positive(message = "Verifier user ID must be positive")
            @RequestParam Integer verifierUserId
    ) {
        DocumentResponse response =
                documentService.rejectDocument(residentDocId, verifierUserId);

        return ResponseEntity.ok(
                success("Document rejected successfully", response)
        );
    }

    // ==========================================================================
    // 7. Delete Document - DELETE /api/documents/{residentDocId}
    // ==========================================================================

    /**
     * Deletes a resident document.
     *
     * <p>The service layer is responsible for deleting the resident-document
     * mapping, document metadata record, and physical file from storage.
     *
     * @param residentDocId resident-document mapping primary key
     * @return success response with no data
     */
    @DeleteMapping("/{residentDocId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SECRETARY') or @ownershipAuthorization.ownsDocument(#residentDocId, authentication)")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @Positive(message = "Document ID must be positive")
            @PathVariable Integer residentDocId
    ) {
        documentService.deleteDocument(residentDocId);

        return ResponseEntity.ok(
                success("Document deleted successfully", null)
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
