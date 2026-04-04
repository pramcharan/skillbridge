package com.skillbridge.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE  = 10 * 1024 * 1024L;
    private static final long MAX_IMAGE_SIZE =  5 * 1024 * 1024L;

    public String uploadChatFile(MultipartFile file,
                                 Long projectId) throws IOException {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("No file provided.");
        if (file.getSize() > MAX_FILE_SIZE)
            throw new IllegalArgumentException("File too large. Max 10 MB.");

        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder",          "skillbridge/chat/" + projectId,
                        "resource_type",   "auto",
                        "use_filename",    true,
                        "unique_filename", true
                )
        );
        String url = (String) result.get("secure_url");
        log.info("Chat file uploaded: {}", url);
        return url;
    }

    public String uploadAvatar(MultipartFile file,
                               Long userId) throws IOException {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("No file provided.");
        if (file.getSize() > MAX_IMAGE_SIZE)
            throw new IllegalArgumentException("Image too large. Max 5 MB.");

        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder",       "skillbridge/avatars",
                        "public_id",    "user_" + userId,
                        "overwrite",    true,
                        "transformation", "c_fill,w_200,h_200,g_face"
                )
        );
        return (String) result.get("secure_url");
    }

    public Map<String, String> uploadJobAttachment(MultipartFile file, Long jobId) {
        if (file.getSize() > 10 * 1024 * 1024)
            throw new RuntimeException("File too large. Max 10MB.");

        String originalName = StringUtils.cleanPath(
                Objects.requireNonNull(file.getOriginalFilename()));

        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", "skillbridge/job-attachments/" + jobId);
            options.put("resource_type", "auto");
            options.put("public_id", UUID.randomUUID().toString());

            Map<?, ?> result = cloudinary.uploader()
                    .upload(file.getBytes(), options);

            return Map.of(
                    "url",  result.get("secure_url").toString(),
                    "name", originalName
            );
        } catch (IOException e) {
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }
}