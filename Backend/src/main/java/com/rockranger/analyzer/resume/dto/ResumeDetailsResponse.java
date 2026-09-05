package com.rockranger.analyzer.resume.dto;

import com.rockranger.analyzer.resume.dto.ai.ParsedResumeResponse;
import com.rockranger.analyzer.resume.entity.ResumeStatus;

import java.time.LocalDateTime;

public class ResumeDetailsResponse {

    private Long id;
    private Long userId;
    private String originalFileName;
    private String cloudinaryUrl;
    private ResumeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ParsedResumeResponse parsedData;

    public ResumeDetailsResponse() {
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

    public ParsedResumeResponse getParsedData() {
        return parsedData;
    }

    public void setParsedData(ParsedResumeResponse parsedData) {
        this.parsedData = parsedData;
    }
}