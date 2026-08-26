package com.rockranger.analyzer.resume.controller;

import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.resume.dto.ResumeResponse;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.processing.ResumeProcessingService;
import com.rockranger.analyzer.resume.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeProcessingService resumeProcessingService;

    public ResumeController(
            ResumeService resumeService,
            ResumeProcessingService resumeProcessingService
    ) {
        this.resumeService = resumeService;
        this.resumeProcessingService = resumeProcessingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResumes(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication required.");
        }

        String email = (authentication.getPrincipal() instanceof User user)
                ? user.getEmail()
                : authentication.getName();

        resumeService.uploadResumes(files, email);

        return ResponseEntity.ok(
                Map.of(
                        "status", "success",
                        "message", files.size() + " resumes uploaded successfully."
                )
        );
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processUploadedResumes() {
        List<Resume> processed = resumeProcessingService.processUploadedResumes();
        return ResponseEntity.ok(
                Map.of(
                        "status", "success",
                        "message", "Processed " + processed.size() + " uploaded resume(s)."
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResumeById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication required.");
        }

        String email = (authentication.getPrincipal() instanceof User user)
                ? user.getEmail()
                : authentication.getName();

        Resume resume = resumeService.getResumeById(id, email);
        return ResponseEntity.ok(ResumeResponse.fromEntity(resume));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ResumeResponse>> getMyResumes(
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication required.");
        }

        String email = (authentication.getPrincipal() instanceof User user)
                ? user.getEmail()
                : authentication.getName();

        List<Resume> resumes = resumeService.getResumesByUserEmail(email);
        List<ResumeResponse> responseList = resumes.stream()
                .map(ResumeResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }
}