package com.rockranger.analyzer.resume.controller;

import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.resume.dto.ResumeResponse;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ResumeResponse> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        Long targetUserId = userId != null ? userId : (authenticatedUser != null ? authenticatedUser.getId() : null);

        if (targetUserId == null) {
            throw new IllegalArgumentException("User ID is required.");
        }

        Resume resume = resumeService.uploadResume(file, targetUserId);
        return ResponseEntity.ok(ResumeResponse.fromEntity(resume));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getResumeById(@PathVariable Long id) {
        Resume resume = resumeService.getResumeById(id);
        return ResponseEntity.ok(ResumeResponse.fromEntity(resume));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ResumeResponse>> getResumesByUserId(@PathVariable Long userId) {
        List<Resume> resumes = resumeService.getResumesByUserId(userId);
        List<ResumeResponse> responseList = resumes.stream()
                .map(ResumeResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }
}