package com.smartsocietyconnect.resident.service;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

/**
 * Service contract for storing and deleting uploaded files.
 *
 * <p>Implementations should handle validation, filename sanitization,
 * physical/cloud storage, and return only the persisted file path or URL
 * that can safely be stored in the database.
 */
public interface FileStorageService {

    /**
     * Stores an uploaded file and returns its stored path or public URL.
     *
     * <p>The implementation should validate that the file is not empty,
     * check allowed content types/extensions, generate a safe unique filename,
     * and prevent path traversal attacks.
     *
     * @param file uploaded multipart file
     * @return stored file path or URL
     */
    String storeFile(MultipartFile file);

    /**
     * Deletes a previously stored file.
     *
     * <p>The implementation should treat missing files as a non-fatal case
     * and return {@code false} when no file was deleted.
     *
     * @param filePath stored file path or URL returned by {@link #storeFile(MultipartFile)}
     * @return {@code true} if the file was deleted, {@code false} otherwise
     */
    boolean deleteFile(String filePath);

    /** Loads a previously stored file without exposing the upload directory. */
    Resource loadFile(String filePath);
}
