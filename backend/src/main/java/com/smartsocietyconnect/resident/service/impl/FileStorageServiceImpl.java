package com.smartsocietyconnect.resident.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smartsocietyconnect.resident.exception.ResidentException;
import com.smartsocietyconnect.resident.service.FileStorageService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Local filesystem implementation of {@link FileStorageService}.
 *
 * <p>This service stores uploaded files inside the configured upload directory.
 * It does not trust client-provided filenames. Instead, it generates a unique
 * filename and only preserves the validated file extension.
 */
@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    /**
     * Maximum allowed upload size: 5 MB.
     */
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    /**
     * File extensions allowed for upload.
     *
     * <p>Keep this list limited to the file types your application actually needs.
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".pdf"
    );

    @Value("${app.upload.dir}")
    private String uploadDir;

    private Path uploadPath;

    /**
     * Initializes and creates the upload directory when the service starts.
     */
    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            log.info("Upload directory initialized: {}", uploadPath);
        } catch (IOException ex) {
            throw new ResidentException("Could not initialize upload directory", ex);
        }
    }

    /**
     * Stores the given multipart file using a generated unique filename.
     *
     * @param file multipart file received from the client
     * @return generated stored filename
     */
    @Override
    public String storeFile(MultipartFile file) {
        validateFile(file);

        String extension = extractAndValidateExtension(file.getOriginalFilename());
        String generatedFileName = UUID.randomUUID() + extension;

        Path targetLocation = uploadPath
                .resolve(generatedFileName)
                .normalize();

        ensurePathInsideUploadDirectory(targetLocation);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File stored successfully: {}", generatedFileName);
            return generatedFileName;
        } catch (IOException ex) {
            throw new ResidentException("Failed to store file", ex);
        }
    }

    /**
     * Deletes a stored file from the upload directory.
     *
     * @param filePath stored filename or relative file path
     * @return {@code true} if deleted, {@code false} if file does not exist or deletion fails
     */
    @Override
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }

        Path target = uploadPath
                .resolve(filePath)
                .normalize();

        ensurePathInsideUploadDirectory(target);

        try {
            boolean deleted = Files.deleteIfExists(target);

            if (deleted) {
                log.info("File deleted successfully: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }

            return deleted;
        } catch (IOException ex) {
            log.error("Failed to delete file: {}", filePath, ex);
            return false;
        }
    }

    @Override
    public Resource loadFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new ResidentException("Document file is missing");
        }

        Path target = uploadPath.resolve(filePath).normalize();
        ensurePathInsideUploadDirectory(target);
        Resource resource = new FileSystemResource(target);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResidentException("Document file is not available");
        }
        return resource;
    }

    /**
     * Validates file presence and file size.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResidentException("Cannot upload empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResidentException("File size must not exceed 5 MB");
        }
    }

    /**
     * Extracts and validates the extension from the original filename.
     *
     * <p>The original filename itself is not used for storage because it may
     * contain unsafe characters or path traversal attempts.
     */
    private String extractAndValidateExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new ResidentException("Original filename is missing");
        }

        String sanitizedFileName = Paths.get(originalFileName)
                .getFileName()
                .toString();

        int extensionIndex = sanitizedFileName.lastIndexOf('.');

        if (extensionIndex < 0) {
            throw new ResidentException("File extension is required");
        }

        String extension = sanitizedFileName
                .substring(extensionIndex)
                .toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResidentException("Unsupported file type: " + extension);
        }

        return extension;
    }

    /**
     * Prevents path traversal outside the configured upload directory.
     */
    private void ensurePathInsideUploadDirectory(Path path) {
        if (!path.startsWith(uploadPath)) {
            throw new ResidentException("Invalid file path");
        }
    }
}
