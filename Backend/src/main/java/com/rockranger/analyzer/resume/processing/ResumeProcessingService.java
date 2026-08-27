package com.rockranger.analyzer.resume.processing;

import com.rockranger.analyzer.resume.entity.Resume;

import java.util.List;

public interface ResumeProcessingService {

    /**
     * Process a single resume.
     */
    void process(Resume resume);

    /**
     * Process a single resume by ID.
     */
    void process(Long resumeId);

    /**
     * Temporary compatibility method.
     *
     * The batch orchestration will eventually live entirely
     * inside ResumeProcessingCoordinator.
     */
    List<Resume> processUploadedResumes();
}