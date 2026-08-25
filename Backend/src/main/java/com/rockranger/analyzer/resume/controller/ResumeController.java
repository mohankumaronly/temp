package com.rockranger.analyzer.resume.controller;

import com.rockranger.analyzer.resume.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId
    ) {

        resumeService.uploadResume(file, userId);

        return ResponseEntity.ok(
                Map.of(
                        "status", "success",
                        "message", "Resume uploaded successfully."
                )
        );
    }
}