package com.rockranger.analyzer.resume.processing;

import com.rockranger.analyzer.resume.ai.ResumeAiParsingService;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeParsedData;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.extraction.ResumeTextExtractionService;
import com.rockranger.analyzer.resume.repository.ResumeParsedDataRepository;
import com.rockranger.analyzer.resume.repository.ResumeRepository;
import com.rockranger.analyzer.resume.storage.ResumeStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeProcessingServiceImpl implements ResumeProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(ResumeProcessingServiceImpl.class);

    private final ResumeStorageService resumeStorageService;
    private final ResumeTextExtractionService resumeTextExtractionService;
    private final ResumeRepository resumeRepository;
    private final ResumeAiParsingService resumeAiParsingService;
    private final ResumeParsedDataRepository resumeParsedDataRepository;

    public ResumeProcessingServiceImpl(
            ResumeStorageService resumeStorageService,
            ResumeTextExtractionService resumeTextExtractionService,
            ResumeRepository resumeRepository,
            ResumeAiParsingService resumeAiParsingService,
            ResumeParsedDataRepository resumeParsedDataRepository
    ) {
        this.resumeStorageService = resumeStorageService;
        this.resumeTextExtractionService = resumeTextExtractionService;
        this.resumeRepository = resumeRepository;
        this.resumeAiParsingService = resumeAiParsingService;
        this.resumeParsedDataRepository = resumeParsedDataRepository;
    }

    @Override
    @Transactional
    public void process(Resume resume) {

        if (resume == null || resume.getId() == null) {
            log.error("Resume or Resume ID is null");
            throw new IllegalArgumentException(
                    "Resume and Resume ID cannot be null."
            );
        }

        log.info(
                "Starting resume processing. Resume ID: {}, File: {}",
                resume.getId(),
                resume.getOriginalFileName()
        );

        process(resume.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long resumeId) {

        log.info("========== RESUME PROCESSING STARTED ==========");
        log.info("Resume ID: {}", resumeId);

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> {
                    log.error("Resume not found with ID: {}", resumeId);
                    return new RuntimeException(
                            "Resume not found with id: " + resumeId
                    );
                });

        log.info(
                "Resume loaded successfully. ID: {}, File: {}, Current Status: {}",
                resume.getId(),
                resume.getOriginalFileName(),
                resume.getStatus()
        );

        try {

            // =========================================================
            // STEP 1: Mark as EXTRACTING
            // =========================================================

            log.info("[STEP 1] Marking resume {} as EXTRACTING", resumeId);

            resume.setStatus(ResumeStatus.EXTRACTING);
            resumeRepository.save(resume);

            log.info("[STEP 1] Resume status updated to EXTRACTING");


            // =========================================================
            // STEP 2: Download PDF
            // =========================================================

            log.info("[STEP 2] Downloading PDF from Cloudinary");
            log.debug(
                    "[STEP 2] Cloudinary URL: {}",
                    resume.getCloudinaryUrl()
            );

            byte[] fileBytes =
                    resumeStorageService.download(
                            resume.getCloudinaryUrl()
                    );

            log.info(
                    "[STEP 2] PDF downloaded successfully. Size: {} bytes",
                    fileBytes != null ? fileBytes.length : 0
            );


            // =========================================================
            // STEP 3: Extract text
            // =========================================================

            log.info("[STEP 3] Starting PDF text extraction");

            String extractedText =
                    resumeTextExtractionService.extractText(fileBytes);

            log.info("[STEP 3] PDF text extraction completed");

            if (extractedText == null) {

                log.error("[STEP 3] Extracted text is NULL");

                throw new IllegalStateException(
                        "No text could be extracted from the resume."
                );
            }

            log.info(
                    "[STEP 3] Extracted text length: {} characters",
                    extractedText.length()
            );

            log.debug(
                    "[STEP 3] Extracted text preview: {}",
                    extractedText.substring(
                            0,
                            Math.min(300, extractedText.length())
                    )
            );


            // =========================================================
            // STEP 4: Validate extracted text
            // =========================================================

            log.info("[STEP 4] Validating extracted text");

            if (extractedText.isBlank()) {

                log.error("[STEP 4] Extracted text is blank");

                throw new IllegalStateException(
                        "No text could be extracted from the resume."
                );
            }

            log.info("[STEP 4] Extracted text validation successful");


            // =========================================================
            // STEP 5: Save extracted text
            // =========================================================

            log.info(
                    "[STEP 5] Saving extracted text to database. Resume ID: {}",
                    resumeId
            );

            resume.setExtractedText(extractedText);
            resume.setStatus(ResumeStatus.PARSING);

            resumeRepository.save(resume);

            log.info(
                    "[STEP 5] Extracted text saved successfully. Status: PARSING"
            );


            // =========================================================
            // STEP 6: Send text to Groq AI
            // =========================================================

            log.info("[STEP 6] Sending resume text to Groq AI");

            log.info(
                    "[STEP 6] Text length being sent to AI: {} characters",
                    extractedText.length()
            );

            String parsedJson =
                    resumeAiParsingService.parseResume(extractedText);

            log.info("[STEP 6] Groq AI response received");


            // =========================================================
            // STEP 7: Validate AI response
            // =========================================================

            if (parsedJson == null) {

                log.error("[STEP 7] Groq returned NULL response");

                throw new IllegalStateException(
                        "Groq AI returned null response."
                );
            }

            if (parsedJson.isBlank()) {

                log.error("[STEP 7] Groq returned EMPTY response");

                throw new IllegalStateException(
                        "Groq AI returned empty response."
                );
            }

            log.info(
                    "[STEP 7] Parsed JSON length: {} characters",
                    parsedJson.length()
            );

            log.debug(
                    "[STEP 7] Parsed JSON response: {}",
                    parsedJson
            );


            // =========================================================
            // STEP 8: Store parsed JSON
            // =========================================================

            log.info(
                    "[STEP 8] Looking for existing parsed data. Resume ID: {}",
                    resumeId
            );

            ResumeParsedData parsedData =
                    resumeParsedDataRepository
                            .findByResumeId(resume.getId())
                            .orElseGet(() -> {
                                log.info(
                                        "[STEP 8] No existing parsed data found. Creating new record."
                                );
                                return new ResumeParsedData();
                            });

            parsedData.setResume(resume);
            parsedData.setParsedJson(parsedJson);

            log.info("[STEP 8] Saving parsed JSON to resume_parsed_data");

            resumeParsedDataRepository.save(parsedData);

            log.info(
                    "[STEP 8] Parsed JSON saved successfully. Resume ID: {}",
                    resumeId
            );


            // =========================================================
            // STEP 9: Mark COMPLETED (UPDATED - FIX FOR CASCADE DELETE)
            // =========================================================

            log.info(
                    "[STEP 9] Marking resume {} as COMPLETED",
                    resumeId
            );

            // ✅ FIX: Use updateStatus() instead of save()
            // This prevents the cascade delete from removing parsed data
            resumeRepository.updateStatus(resumeId, ResumeStatus.COMPLETED);

            log.info(
                    "[STEP 9] Resume status updated to COMPLETED successfully"
            );
            log.info(
                    "[STEP 9] Parsed JSON data preserved in resume_parsed_data table"
            );

            log.info("========== RESUME PROCESSING SUCCESS ==========");

        } catch (Exception e) {

            log.error(
                    "========== RESUME PROCESSING FAILED =========="
            );

            log.error(
                    "Resume ID: {}",
                    resumeId
            );

            log.error(
                    "File: {}",
                    resume.getOriginalFileName()
            );

            log.error(
                    "Current status before failure: {}",
                    resume.getStatus()
            );

            log.error(
                    "Exception type: {}",
                    e.getClass().getName()
            );

            log.error(
                    "Exception message: {}",
                    e.getMessage()
            );

            log.error(
                    "Full exception:",
                    e
            );

            try {

                // ✅ FIX: Use updateStatus() for FAILED status too
                resumeRepository.updateStatus(resumeId, ResumeStatus.FAILED);

                log.info(
                        "Resume {} marked as FAILED",
                        resumeId
                );

            } catch (Exception statusException) {

                log.error(
                        "Could not update resume {} status to FAILED",
                        resumeId,
                        statusException
                );
            }

            throw new RuntimeException(
                    "Failed to process resume: "
                            + resume.getOriginalFileName(),
                    e
            );
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public java.util.List<Resume> processUploadedResumes() {

        log.info("========== PROCESSING UPLOADED RESUMES ==========");

        java.util.List<Resume> uploadedResumes =
                resumeRepository.findByStatus(
                        ResumeStatus.UPLOADED
                );

        log.info(
                "Found {} uploaded resumes",
                uploadedResumes.size()
        );

        java.util.List<Resume> processedResumes =
                new java.util.ArrayList<>();

        for (Resume resume : uploadedResumes) {

            log.info(
                    "Starting processing for Resume ID: {}, File: {}",
                    resume.getId(),
                    resume.getOriginalFileName()
            );

            try {

                process(resume.getId());

                log.info(
                        "Successfully processed Resume ID: {}",
                        resume.getId()
                );

            } catch (Exception e) {

                log.error(
                        "Failed processing Resume ID: {}, File: {}",
                        resume.getId(),
                        resume.getOriginalFileName(),
                        e
                );
            }

            resumeRepository
                    .findById(resume.getId())
                    .ifPresent(processedResumes::add);
        }

        log.info(
                "========== UPLOADED RESUMES PROCESSING FINISHED =========="
        );

        return processedResumes;
    }
}