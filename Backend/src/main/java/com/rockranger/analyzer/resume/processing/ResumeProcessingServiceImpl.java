package com.rockranger.analyzer.resume.processing;

import com.rockranger.analyzer.resume.ai.ResumeAiParsingService;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeParsedData;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.extraction.ResumeExtractionRouter;
import com.rockranger.analyzer.resume.repository.ResumeParsedDataRepository;
import com.rockranger.analyzer.resume.repository.ResumeRepository;
import com.rockranger.analyzer.resume.storage.ResumeStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResumeProcessingServiceImpl
        implements ResumeProcessingService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ResumeProcessingServiceImpl.class
            );

    private final ResumeStorageService resumeStorageService;
    private final ResumeExtractionRouter resumeExtractionRouter;
    private final ResumeRepository resumeRepository;
    private final ResumeAiParsingService resumeAiParsingService;
    private final ResumeParsedDataRepository resumeParsedDataRepository;


    public ResumeProcessingServiceImpl(
            ResumeStorageService resumeStorageService,
            ResumeExtractionRouter resumeExtractionRouter,
            ResumeRepository resumeRepository,
            ResumeAiParsingService resumeAiParsingService,
            ResumeParsedDataRepository resumeParsedDataRepository
    ) {

        this.resumeStorageService = resumeStorageService;
        this.resumeExtractionRouter = resumeExtractionRouter;
        this.resumeRepository = resumeRepository;
        this.resumeAiParsingService = resumeAiParsingService;
        this.resumeParsedDataRepository =
                resumeParsedDataRepository;
    }


    // =========================================================
    // PROCESS SINGLE RESUME OBJECT
    // =========================================================

    @Override
    @Transactional
    public void process(Resume resume) {

        if (resume == null || resume.getId() == null) {

            throw new IllegalArgumentException(
                    "Resume and Resume ID cannot be null."
            );
        }

        log.info(
                "Starting processing for Resume ID: {}",
                resume.getId()
        );

        process(resume.getId());
    }


    // =========================================================
    // PROCESS SINGLE RESUME BY ID
    // =========================================================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long resumeId) {

        if (resumeId == null) {

            throw new IllegalArgumentException(
                    "Resume ID cannot be null."
            );
        }

        log.info(
                "========== RESUME PROCESSING STARTED =========="
        );

        log.info(
                "Resume ID: {}",
                resumeId
        );


        Resume resume =
                resumeRepository.findById(resumeId)
                        .orElseThrow(() -> {

                            log.error(
                                    "Resume not found with ID: {}",
                                    resumeId
                            );

                            return new RuntimeException(
                                    "Resume not found with id: "
                                            + resumeId
                            );
                        });


        try {

            // =====================================================
            // STEP 1 — EXTRACTING
            // =====================================================

            log.info(
                    "[STEP 1] Resume {} → EXTRACTING",
                    resumeId
            );

            resume.setStatus(
                    ResumeStatus.EXTRACTING
            );

            resumeRepository.save(resume);


            // =====================================================
            // STEP 2 — DOWNLOAD FILE
            // =====================================================

            log.info(
                    "[STEP 2] Downloading resume file"
            );

            byte[] fileBytes =
                    resumeStorageService.download(
                            resume.getCloudinaryUrl()
                    );

            if (fileBytes == null || fileBytes.length == 0) {

                throw new IllegalStateException(
                        "Downloaded resume file is empty."
                );
            }

            log.info(
                    "[STEP 2] Download successful. Size: {} bytes",
                    fileBytes.length
            );


            // =====================================================
            // STEP 3 — TEXT EXTRACTION
            // =====================================================

            log.info(
                    "[STEP 3] Starting text extraction"
            );

            String extractedText =
                    resumeExtractionRouter.extract(
                            fileBytes,
                            resume.getOriginalFileName()
                    );

            if (extractedText == null
                    || extractedText.isBlank()) {

                throw new IllegalStateException(
                        "No text could be extracted from the resume."
                );
            }

            log.info(
                    "[STEP 3] Text extraction successful. Length: {}",
                    extractedText.length()
            );


            // =====================================================
            // STEP 4 — STORE EXTRACTED TEXT
            // =====================================================

            log.info(
                    "[STEP 4] Saving extracted text"
            );

            resume.setExtractedText(
                    extractedText
            );

            resume.setStatus(
                    ResumeStatus.PARSING
            );

            resumeRepository.save(resume);


            // =====================================================
            // STEP 5 — AI PARSING
            // =====================================================

            log.info(
                    "[STEP 5] Sending resume text to AI"
            );

            String parsedJson =
                    resumeAiParsingService.parseResume(
                            extractedText
                    );

            if (parsedJson == null
                    || parsedJson.isBlank()) {

                throw new IllegalStateException(
                        "AI returned empty parsed data."
                );
            }

            log.info(
                    "[STEP 5] AI parsing completed. JSON length: {}",
                    parsedJson.length()
            );


            // =====================================================
            // STEP 6 — STORE PARSED DATA
            // =====================================================

            log.info(
                    "[STEP 6] Saving parsed resume JSON"
            );

            ResumeParsedData parsedData =
                    resumeParsedDataRepository
                            .findByResumeId(
                                    resumeId
                            )
                            .orElseGet(
                                    ResumeParsedData::new
                            );

            parsedData.setResume(
                    resume
            );

            parsedData.setParsedJson(
                    parsedJson
            );

            resumeParsedDataRepository.save(
                    parsedData
            );


            // =====================================================
            // STEP 7 — COMPLETED
            // =====================================================

            log.info(
                    "[STEP 7] Marking Resume {} as COMPLETED",
                    resumeId
            );

            resumeRepository.updateStatus(
                    resumeId,
                    ResumeStatus.COMPLETED
            );

            log.info(
                    "========== RESUME PROCESSING COMPLETED =========="
            );


        } catch (Exception e) {

            log.error(
                    "========== RESUME PROCESSING FAILED =========="
            );

            log.error(
                    "Resume ID: {}",
                    resumeId
            );

            log.error(
                    "Processing error:",
                    e
            );


            try {

                resumeRepository.updateStatus(
                        resumeId,
                        ResumeStatus.FAILED
                );

            } catch (Exception statusException) {

                log.error(
                        "Failed to mark Resume {} as FAILED",
                        resumeId,
                        statusException
                );
            }


            throw new RuntimeException(
                    "Failed to process resume with ID: "
                            + resumeId,
                    e
            );
        }
    }


    // =========================================================
    // TEMPORARY COMPATIBILITY METHOD
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<Resume> processUploadedResumes() {

        /*
         * Batch orchestration now belongs to
         * ResumeProcessingCoordinator.
         *
         * This method is retained temporarily so existing
         * callers do not immediately break.
         *
         * We intentionally do NOT start threads here.
         */

        return resumeRepository.findByStatus(
                ResumeStatus.UPLOADED
        );
    }
}