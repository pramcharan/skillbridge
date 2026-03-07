package com.skillbridge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-size-mb:5}")
    private int maxSizeMb;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final List<String> ALLOWED_DOC_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "image/jpeg", "image/png", "image/webp"
    );

    // ── Store avatar ──────────────────────────────────────────────────
    public String storeAvatar(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES);
        return store(file, "avatars");
    }

    // ── Store portfolio file ──────────────────────────────────────────
    public String storePortfolioFile(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_DOC_TYPES);
        return store(file, "portfolio");
    }

    // ── Delete a file ─────────────────────────────────────────────────
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) return;
        try {
            String relativePath = fileUrl.substring(1); // remove leading /
            Path filePath = Paths.get(relativePath).toAbsolutePath().normalize();
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", fileUrl);
        }
    }

    // ── Internal ──────────────────────────────────────────────────────
    private String store(MultipartFile file, String subfolder) throws IOException {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null
                        ? file.getOriginalFilename() : "file");
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex).toLowerCase();
        }

        String filename = UUID.randomUUID() + extension;
        Path targetDir  = Paths.get(uploadDir, subfolder).toAbsolutePath().normalize();
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(filename);
        Files.copy(file.getInputStream(), targetPath,
                StandardCopyOption.REPLACE_EXISTING);

        log.info("Stored file: {}", targetPath);
        return "/uploads/" + subfolder + "/" + filename;
    }

    private void validateFile(MultipartFile file,
                              List<String> allowedTypes) {
        if (file.isEmpty()) {
            throw new com.skillbridge.exception.BadRequestException(
                    "File is empty");
        }
        long maxBytes = (long) maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new com.skillbridge.exception.BadRequestException(
                    "File exceeds max size of " + maxSizeMb + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new com.skillbridge.exception.BadRequestException(
                    "File type not allowed: " + contentType);
        }
    }
}