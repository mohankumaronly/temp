package com.rockranger.analyzer.resume.controller;

import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.resume.dto.ResumeDetailsResponse;
import com.rockranger.analyzer.resume.dto.ResumeResponse;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.processing.ResumeProcessingCoordinator;
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

    private final ResumeProcessingCoordinator
            resumeProcessingCoordinator;

    public ResumeController(
            ResumeService resumeService,
            ResumeProcessingCoordinator resumeProcessingCoordinator
    ) {

        this.resumeService = resumeService;

        this.resumeProcessingCoordinator =
                resumeProcessingCoordinator;
    }

    // =========================================================
    // UPLOAD RESUMES
    // =========================================================

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResumes(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication
    ) {

        if (authentication == null) {
            throw new IllegalArgumentException(
                    "Authentication required."
            );
        }

        String email =
                (authentication.getPrincipal() instanceof User user)
                        ? user.getEmail()
                        : authentication.getName();

        resumeService.uploadResumes(
                files,
                email
        );

        return ResponseEntity.ok(
                Map.of(
                        "status",
                        "success",

                        "message",
                        files.size()
                                + " resumes uploaded successfully."
                )
        );
    }

    // =========================================================
    // PROCESS UPLOADED RESUMES
    // =========================================================

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>>
    processUploadedResumes() {

        /*
         * IMPORTANT:
         *
         * The controller does NOT process resumes directly.
         *
         * It asks the Coordinator to submit the resumes.
         *
         * Coordinator
         *      ↓
         * Executor
         *      ↓
         * ProcessingService
         *      ↓
         * Extraction → AI → Database
         */

        List<Resume> submitted =
                resumeProcessingCoordinator
                        .submitUploadedResumes();

        return ResponseEntity.ok(
                Map.of(
                        "status",
                        "success",

                        "message",
                        submitted.size()
                                + " resume(s) submitted for processing."
                )
        );
    }

    // =========================================================
    // GET RESUME BY ID WITH PARSED DATA
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<ResumeDetailsResponse>
    getResumeById(
            @PathVariable Long id,
            Authentication authentication
    ) {

        if (authentication == null) {
            throw new IllegalArgumentException(
                    "Authentication required."
            );
        }

        String email =
                (authentication.getPrincipal() instanceof User user)
                        ? user.getEmail()
                        : authentication.getName();

        ResumeDetailsResponse response =
                resumeService.getResumeDetailsById(
                        id,
                        email
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET MY RESUMES
    // =========================================================

    @GetMapping("/my")
    public ResponseEntity<List<ResumeResponse>>
    getMyResumes(
            Authentication authentication
    ) {

        if (authentication == null) {
            throw new IllegalArgumentException(
                    "Authentication required."
            );
        }

        String email =
                (authentication.getPrincipal() instanceof User user)
                        ? user.getEmail()
                        : authentication.getName();

        List<Resume> resumes =
                resumeService.getResumesByUserEmail(
                        email
                );

        List<ResumeResponse> responseList =
                resumes.stream()
                        .map(ResumeResponse::fromEntity)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(
                responseList
        );
    }
}