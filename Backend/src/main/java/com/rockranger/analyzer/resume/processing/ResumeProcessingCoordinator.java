package com.rockranger.analyzer.resume.processing;

import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.repository.ResumeRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResumeProcessingCoordinator {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ResumeProcessingCoordinator.class
            );

    private final ResumeRepository resumeRepository;
    private final ResumeProcessingExecutor processingExecutor;

    public ResumeProcessingCoordinator(
            ResumeRepository resumeRepository,
            ResumeProcessingExecutor processingExecutor
    ) {

        this.resumeRepository = resumeRepository;
        this.processingExecutor = processingExecutor;
    }

    /**
     * Find uploaded resumes and submit them
     * for asynchronous processing.
     */
    public List<Resume> submitUploadedResumes() {

        log.info(
                "========== RESUME BATCH SUBMISSION STARTED =========="
        );

        List<Resume> uploadedResumes =
                resumeRepository.findByStatus(
                        ResumeStatus.UPLOADED
                );

        log.info(
                "Found {} uploaded resume(s)",
                uploadedResumes.size()
        );

        for (Resume resume : uploadedResumes) {

            if (resume == null || resume.getId() == null) {

                log.warn(
                        "Skipping invalid resume entry"
                );

                continue;
            }

            log.info(
                    "Submitting Resume ID: {} for background processing",
                    resume.getId()
            );

            processingExecutor.submit(
                    resume.getId()
            );
        }

        log.info(
                "========== RESUME BATCH SUBMISSION COMPLETED =========="
        );

        return uploadedResumes;
    }

    /**
     * Submit one resume for asynchronous processing.
     */
    public void submitResume(Long resumeId) {

        if (resumeId == null) {
            throw new IllegalArgumentException(
                    "Resume ID cannot be null."
            );
        }

        log.info(
                "Submitting single Resume ID: {}",
                resumeId
        );

        processingExecutor.submit(
                resumeId
        );
    }
}