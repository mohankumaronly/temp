package com.rockranger.analyzer.resume.dto;

import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeStatus;

import java.time.LocalDateTime;

public class ResumeResponse {

    private Long id;
    private Long userId;
    private String originalFileName;
    private String cloudinaryUrl;
    private ResumeStatus status;
    private String extractedText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ResumeResponse() {
    }

    public static ResumeResponse fromEntity(Resume resume) {
        if (resume == null) {
            return null;
        }

        ResumeResponse dto = new ResumeResponse();
        dto.setId(resume.getId());
        if (resume.getUser() != null) {
            dto.setUserId(resume.getUser().getId());
        }
        dto.setOriginalFileName(resume.getOriginalFileName());
        dto.setCloudinaryUrl(resume.getCloudinaryUrl());
        dto.setStatus(resume.getStatus());
        dto.setExtractedText(resume.getExtractedText());
        dto.setCreatedAt(resume.getCreatedAt());
        dto.setUpdatedAt(resume.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getCloudinaryUrl() {
        return cloudinaryUrl;
    }

    public void setCloudinaryUrl(String cloudinaryUrl) {
        this.cloudinaryUrl = cloudinaryUrl;
    }

    public ResumeStatus getStatus() {
        return status;
    }

    public void setStatus(ResumeStatus status) {
        this.status = status;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
