package com.skillbridge.controller;

import com.skillbridge.exception.BadRequestException;
import com.skillbridge.repository.UserRepository;
import com.skillbridge.service.FileUploadService;
import com.skillbridge.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final UserRepository userRepository;
    private final JobService jobService;

    // POST /api/v1/upload/chat/{projectId}
    @PostMapping("/chat/{projectId}")
    public ResponseEntity<Map<String,String>> uploadChatFile(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String email) {
        try {
            String url = fileUploadService
                    .uploadChatFile(file, projectId);
            return ResponseEntity.ok(Map.of("url", url,
                    "fileName", file.getOriginalFilename(),
                    "fileType", file.getContentType() != null
                            ? file.getContentType() : "unknown"));
        } catch (Exception e) {
            throw new BadRequestException(
                    "Upload failed: " + e.getMessage());
        }
    }

    // POST /api/v1/upload/avatar
    @PostMapping("/avatar")
    public ResponseEntity<Map<String,String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String email) {
        try {
            // get userId from userRepository via email
            String url = fileUploadService.uploadAvatar(file, 0L);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            throw new BadRequestException(
                    "Upload failed: " + e.getMessage());
        }
    }

    // Add to FileUploadController

    @PostMapping("/job/{jobId}")
    public ResponseEntity<Map<String, String>> uploadJobAttachment(
            @PathVariable Long jobId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal String email) {

        Map<String, String> uploaded = fileUploadService.uploadJobAttachment(file, jobId);
        jobService.addAttachment(jobId, uploaded.get("url"), uploaded.get("name"), email);
        return ResponseEntity.ok(uploaded);
    }
}