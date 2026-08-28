package com.rockranger.analyzer.resume.processing;

import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeParsedData;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.repository.ResumeParsedDataRepository;
import com.rockranger.analyzer.resume.repository.ResumeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeProcessingPersistenceService {

    private final ResumeRepository resumeRepository;
    private final ResumeParsedDataRepository resumeParsedDataRepository;


    public ResumeProcessingPersistenceService(
            ResumeRepository resumeRepository,
            ResumeParsedDataRepository resumeParsedDataRepository
    ) {

        this.resumeRepository = resumeRepository;
        this.resumeParsedDataRepository =
                resumeParsedDataRepository;
    }


    /**
     * Mark resume as EXTRACTING.
     *
     * This transaction commits independently.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExtracting(Long resumeId) {

        Resume resume =
                getResume(resumeId);

        resume.setStatus(
                ResumeStatus.EXTRACTING
        );

        resumeRepository.save(resume);
    }


    /**
     * Save extracted text independently.
     *
     * IMPORTANT:
     *
     * Once this method returns successfully,
     * the extracted text is committed to the database.
     *
     * A later AI failure cannot roll it back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveExtractedText(
            Long resumeId,
            String extractedText
    ) {

        if (extractedText == null
                || extractedText.isBlank()) {

            throw new IllegalArgumentException(
                    "Extracted resume text cannot be empty."
            );
        }


        Resume resume =
                getResume(resumeId);

        resume.setExtractedText(
                extractedText
        );

        resume.setStatus(
                ResumeStatus.PARSING
        );

        resumeRepository.save(resume);
    }


    /**
     * Save parsed AI JSON independently.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveParsedData(
            Long resumeId,
            String parsedJson
    ) {

        if (parsedJson == null
                || parsedJson.isBlank()) {

            throw new IllegalArgumentException(
                    "Parsed resume JSON cannot be empty."
            );
        }


        Resume resume =
                getResume(resumeId);


        ResumeParsedData parsedData =
                resumeParsedDataRepository
                        .findByResumeId(resumeId)
                        .orElseGet(
                                ResumeParsedData::new
                        );


        parsedData.setResume(resume);

        parsedData.setParsedJson(
                parsedJson
        );


        resumeParsedDataRepository.save(
                parsedData
        );
    }


    /**
     * Mark resume as COMPLETED.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(Long resumeId) {

        getResume(resumeId);

        resumeRepository.updateStatus(
                resumeId,
                ResumeStatus.COMPLETED
        );
    }


    /**
     * Mark resume as FAILED.
     *
     * This is deliberately a separate transaction.
     *
     * Therefore it can still commit even when
     * the AI-processing operation has failed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long resumeId) {

        getResume(resumeId);

        resumeRepository.updateStatus(
                resumeId,
                ResumeStatus.FAILED
        );
    }


    /**
     * Load resume by ID.
     */
    private Resume getResume(Long resumeId) {

        if (resumeId == null) {

            throw new IllegalArgumentException(
                    "Resume ID cannot be null."
            );
        }


        return resumeRepository
                .findById(resumeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Resume not found with id: "
                                        + resumeId
                        )
                );
    }
}