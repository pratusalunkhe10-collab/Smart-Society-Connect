package com.smartsocietyconnect.resident.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smartsocietyconnect.auth.entity.User;
import com.smartsocietyconnect.auth.repository.UserRepository;
import com.smartsocietyconnect.resident.dto.request.UploadDocumentRequest;
import com.smartsocietyconnect.resident.dto.response.DocumentResponse;
import com.smartsocietyconnect.resident.entity.Document;
import com.smartsocietyconnect.resident.entity.Resident;
import com.smartsocietyconnect.resident.entity.ResidentDocument;
import com.smartsocietyconnect.resident.entity.VerificationStatus;
import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.resident.repository.DocumentRepository;
import com.smartsocietyconnect.resident.repository.ResidentDocumentRepository;
import com.smartsocietyconnect.resident.repository.ResidentRepository;
import com.smartsocietyconnect.resident.service.DocumentService;
import com.smartsocietyconnect.resident.service.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for resident document management.
 *
 * <p>Handles document upload, metadata persistence, resident-document mapping,
 * verification workflow, and deletion of both DB records and physical files.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final ResidentDocumentRepository residentDocumentRepository;
    private final ResidentRepository residentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    /**
     * Uploads a document for a resident.
     *
     * <p>The physical file is stored first. If database persistence fails after
     * file storage, the stored file is deleted to avoid orphan files.
     */
    @Override
    public DocumentResponse uploadDocument(UploadDocumentRequest request, MultipartFile file) {
        validateUploadRequest(request);

        Resident resident = findResidentById(request.getResidentId());
        String storedFilePath = null;

        try {
            storedFilePath = fileStorageService.storeFile(file);

            Document document = Document.builder()
                    .documentName(request.getDocumentName())
                    .documentType(request.getDocumentType())
                    .mimeType(file.getContentType())
                    .filePath(storedFilePath)
                    .fileSize(Math.toIntExact(file.getSize()))
                    .build();

            Document savedDocument = documentRepository.save(document);

            ResidentDocument residentDocument = ResidentDocument.builder()
                    .resident(resident)
                    .document(savedDocument)
                    .uploadedBy(resident.getUser())
                    .verificationStatus(VerificationStatus.PENDING)
                    .build();

            ResidentDocument savedResidentDocument =
                    residentDocumentRepository.save(residentDocument);

            log.info(
                    "Document uploaded successfully. residentId={}, documentId={}",
                    resident.getResidentId(),
                    savedDocument.getDocumentId()
            );

            return mapToResponse(savedResidentDocument);
        } catch (RuntimeException ex) {
            cleanupStoredFileAfterFailure(storedFilePath);
            throw ex;
        }
    }

    /**
     * Fetches one resident-document mapping by ID.
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Integer residentDocId) {
        return mapToResponse(findResidentDocumentById(residentDocId));
    }

    /**
     * Fetches all documents linked to a resident.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByResident(Integer residentId) {
        if (!residentRepository.existsById(residentId)) {
            throw new ResidentException("Resident not found with id: " + residentId);
        }

        return residentDocumentRepository.findByResidentResidentId(residentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Fetches documents by verification status.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByStatus(VerificationStatus status) {
        if (status == null) {
            throw new ResidentException("Verification status is required");
        }

        return residentDocumentRepository.findByVerificationStatus(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments() {
        return residentDocumentRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadDocumentFile(Integer residentDocId) {
        return fileStorageService.loadFile(
                findResidentDocumentById(residentDocId).getDocument().getFilePath()
        );
    }

    /**
     * Marks a pending document as verified.
     */
    @Override
    public DocumentResponse verifyDocument(Integer residentDocId, Integer verifierUserId) {
        ResidentDocument residentDocument = findResidentDocumentById(residentDocId);
        User verifier = findVerifierById(verifierUserId);

        updateVerificationStatus(
                residentDocument,
                verifier,
                VerificationStatus.VERIFIED
        );

        ResidentDocument saved = residentDocumentRepository.save(residentDocument);

        log.info(
                "Document verified. residentDocId={}, verifierUserId={}",
                residentDocId,
                verifierUserId
        );

        return mapToResponse(saved);
    }

    /**
     * Marks a pending document as rejected.
     */
    @Override
    public DocumentResponse rejectDocument(Integer residentDocId, Integer verifierUserId) {
        ResidentDocument residentDocument = findResidentDocumentById(residentDocId);
        User verifier = findVerifierById(verifierUserId);

        updateVerificationStatus(
                residentDocument,
                verifier,
                VerificationStatus.REJECTED
        );

        ResidentDocument saved = residentDocumentRepository.save(residentDocument);

        log.info(
                "Document rejected. residentDocId={}, verifierUserId={}",
                residentDocId,
                verifierUserId
        );

        return mapToResponse(saved);
    }

    /**
     * Deletes resident-document mapping, document metadata, and physical file.
     *
     * <p>If physical file deletion fails, the failure is logged by
     * {@link FileStorageService}; the database delete is still completed.
     */
    @Override
    public void deleteDocument(Integer residentDocId) {
        ResidentDocument residentDocument = findResidentDocumentById(residentDocId);
        Document document = residentDocument.getDocument();
        String filePath = document.getFilePath();

        residentDocumentRepository.delete(residentDocument);
        documentRepository.delete(document);

        boolean fileDeleted = fileStorageService.deleteFile(filePath);

        if (fileDeleted) {
            log.info("Document deleted successfully. residentDocId={}", residentDocId);
        } else {
            log.warn(
                    "Document DB records deleted, but physical file was not deleted. filePath={}",
                    filePath
            );
        }
    }

    /**
     * Validates required upload request values.
     *
     * <p>Controller-level {@code @Valid} should already handle this, but service
     * validation keeps business logic safe even if called internally.
     */
    private void validateUploadRequest(UploadDocumentRequest request) {
        if (request == null) {
            throw new ResidentException("Upload document request is required");
        }

        if (request.getResidentId() == null) {
            throw new ResidentException("Resident ID is required");
        }

        if (request.getDocumentType() == null) {
            throw new ResidentException("Document type is required");
        }

        if (request.getDocumentName() == null || request.getDocumentName().isBlank()) {
            throw new ResidentException("Document name is required");
        }
    }

    /**
     * Finds resident or throws a domain-specific exception.
     */
    private Resident findResidentById(Integer residentId) {
        return residentRepository.findById(residentId)
                .orElseThrow(() -> new ResidentException(
                        "Resident not found with id: " + residentId));
    }

    /**
     * Finds resident-document mapping or throws a domain-specific exception.
     */
    private ResidentDocument findResidentDocumentById(Integer residentDocId) {
        return residentDocumentRepository.findById(residentDocId)
                .orElseThrow(() -> new ResidentException(
                        "Document not found with id: " + residentDocId));
    }

    /**
     * Finds verifier user or throws a domain-specific exception.
     */
    private User findVerifierById(Integer verifierUserId) {
        return userRepository.findById(verifierUserId)
                .orElseThrow(() -> new ResidentException(
                        "Verifier not found with id: " + verifierUserId));
    }

    /**
     * Applies verification decision to a pending document.
     *
     * <p>Only pending documents can be verified or rejected. This prevents
     * accidental overwriting of previous admin decisions.
     */
    private void updateVerificationStatus(
            ResidentDocument residentDocument,
            User verifier,
            VerificationStatus targetStatus
    ) {
        if (!VerificationStatus.PENDING.equals(residentDocument.getVerificationStatus())) {
            throw new ResidentException(
                    "Only pending documents can be verified or rejected"
            );
        }

        residentDocument.setVerificationStatus(targetStatus);
        residentDocument.setVerifiedBy(verifier);
        residentDocument.setVerifiedAt(LocalDateTime.now());
    }

    /**
     * Deletes a stored file when DB persistence fails after file upload.
     */
    private void cleanupStoredFileAfterFailure(String storedFilePath) {
        if (storedFilePath == null) {
            return;
        }

        boolean deleted = fileStorageService.deleteFile(storedFilePath);

        if (!deleted) {
            log.warn(
                    "Failed to clean up stored file after upload failure. filePath={}",
                    storedFilePath
            );
        }
    }

    /**
     * Maps ResidentDocument entity to response DTO.
     */
    private DocumentResponse mapToResponse(ResidentDocument residentDocument) {
        Document document = residentDocument.getDocument();

        return DocumentResponse.builder()
                .residentDocId(residentDocument.getResidentDocId())
                .residentId(residentDocument.getResident().getResidentId())
                .residentName(String.join(" ",
                        residentDocument.getResident().getUser().getFirstName(),
                        residentDocument.getResident().getUser().getLastName() == null
                                ? "" : residentDocument.getResident().getUser().getLastName()).trim())
                .documentId(document.getDocumentId())
                .documentName(document.getDocumentName())
                .documentType(document.getDocumentType())
                .mimeType(document.getMimeType())
                .filePath(document.getFilePath())
                .fileSize(document.getFileSize())
                .verificationStatus(residentDocument.getVerificationStatus())
                .uploadedBy(residentDocument.getUploadedBy().getUserId())
                .verifiedBy(residentDocument.getVerifiedBy() != null
                        ? residentDocument.getVerifiedBy().getUserId()
                        : null)
                .verifiedAt(residentDocument.getVerifiedAt())
                .createdAt(residentDocument.getCreatedAt())
                .build();
    }
}
