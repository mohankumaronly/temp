package com.rockranger.analyzer.resume.processing;

import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.extraction.ResumeTextExtractionService;
import com.rockranger.analyzer.resume.repository.ResumeRepository;
import com.rockranger.analyzer.resume.storage.ResumeStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeProcessingServiceImpl implements ResumeProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ResumeProcessingServiceImpl.class);

    private final ResumeStorageService resumeStorageService;
    private final ResumeTextExtractionService resumeTextExtractionService;
    private final ResumeRepository resumeRepository;

    public ResumeProcessingServiceImpl(
            ResumeStorageService resumeStorageService,
            ResumeTextExtractionService resumeTextExtractionService,
            ResumeRepository resumeRepository
    ) {
        this.resumeStorageService = resumeStorageService;
        this.resumeTextExtractionService = resumeTextExtractionService;
        this.resumeRepository = resumeRepository;
    }

    @Override
    @Transactional
    public void process(Resume resume) {
        if (resume == null || resume.getId() == null) {
            throw new IllegalArgumentException("Resume and Resume ID cannot be null.");
        }
        process(resume.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long resumeId) {

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found with id: " + resumeId));

        try {

            // 1. Mark resume as extracting
            resume.setStatus(ResumeStatus.EXTRACTING);
            resumeRepository.save(resume);

            // 2. Download PDF from Cloudinary
            byte[] fileBytes =
                    resumeStorageService.download(resume.getCloudinaryUrl());

            // 3. Extract text from PDF
            String extractedText =
                    resumeTextExtractionService.extractText(fileBytes);

            // 4. Validate extracted text
            if (extractedText == null || extractedText.isBlank()) {
                throw new IllegalStateException(
                        "No text could be extracted from the resume."
                );
            }

            // 5. Save extracted text
            resume.setExtractedText(extractedText);

            // 6. Mark processing as completed
            resume.setStatus(ResumeStatus.COMPLETED);

            resumeRepository.save(resume);

        } catch (Exception e) {

            resume.setStatus(ResumeStatus.FAILED);
            resumeRepository.save(resume);

            throw new RuntimeException(
                    "Failed to process resume: " + resume.getOriginalFileName(),
                    e
            );
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public java.util.List<Resume> processUploadedResumes() {
        java.util.List<Resume> uploadedResumes = resumeRepository.findByStatus(ResumeStatus.UPLOADED);
        java.util.List<Resume> processedResumes = new java.util.ArrayList<>();

        for (Resume resume : uploadedResumes) {
            try {
                process(resume.getId());
            } catch (Exception e) {
                log.error("Failed processing resume {}: {}", resume.getId(), e.getMessage(), e);
            }
            resumeRepository.findById(resume.getId()).ifPresent(processedResumes::add);
        }

        return processedResumes;
    }
}