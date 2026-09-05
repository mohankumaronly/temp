package com.rockranger.analyzer.resume.service.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.authentication.repository.UserRepository;
import com.rockranger.analyzer.resume.dto.ResumeDetailsResponse;
import com.rockranger.analyzer.resume.dto.ai.ParsedResumeResponse;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeParsedData;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.repository.ResumeRepository;
import com.rockranger.analyzer.resume.service.ResumeService;
import com.rockranger.analyzer.resume.storage.ResumeStorageService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeStorageService resumeStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeServiceImpl(
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            ResumeStorageService resumeStorageService
    ) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.resumeStorageService = resumeStorageService;
    }

    // =========================================================
    // UPLOAD RESUMES
    // =========================================================

    @Override
    @Transactional
    public List<Resume> uploadResumes(
            List<MultipartFile> files,
            String email
    ) {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one resume is required."
            );
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found."
                        )
                );

        List<Resume> uploadedResumes =
                new ArrayList<>();

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException(
                        "Resume file cannot be empty."
                );
            }

            String originalFilename =
                    file.getOriginalFilename();

            String contentType =
                    file.getContentType();

            boolean isPdfByExtension =
                    originalFilename != null
                            && originalFilename
                            .toLowerCase()
                            .endsWith(".pdf");

            boolean isPdfByContentType =
                    contentType != null
                            && (
                            contentType.equalsIgnoreCase(
                                    "application/pdf"
                            )
                                    || contentType.equalsIgnoreCase(
                                    "application/x-pdf"
                            )
                    );

            if (!isPdfByExtension
                    && !isPdfByContentType) {

                throw new IllegalArgumentException(
                        "Only PDF resumes are supported: "
                                + originalFilename
                );
            }

            // =================================================
            // 1. Upload PDF to Cloudinary
            // =================================================

            String cloudinaryUrl =
                    resumeStorageService.upload(file);

            // =================================================
            // 2. Create Resume record
            // =================================================

            Resume resume = new Resume();

            resume.setUser(user);

            resume.setOriginalFileName(
                    originalFilename != null
                            ? originalFilename
                            : "resume.pdf"
            );

            resume.setCloudinaryUrl(
                    cloudinaryUrl
            );

            resume.setStatus(
                    ResumeStatus.UPLOADED
            );

            // =================================================
            // 3. Save resume metadata
            // =================================================

            resume =
                    resumeRepository.save(resume);

            uploadedResumes.add(resume);
        }

        return uploadedResumes;
    }

    // =========================================================
    // GET RESUME BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Resume getResumeById(
            Long id,
            String email
    ) {

        return resumeRepository
                .findByIdAndUserEmail(id, email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Resume not found with id: "
                                        + id
                        )
                );
    }

    // =========================================================
    // GET RESUME DETAILS BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public ResumeDetailsResponse getResumeDetailsById(
            Long id,
            String email
    ) {

        Resume resume =
                resumeRepository
                        .findByIdAndUserEmail(
                                id,
                                email
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Resume not found with id: "
                                                + id
                                )
                        );

        ResumeDetailsResponse response =
                new ResumeDetailsResponse();

        // =====================================================
        // Resume metadata
        // =====================================================

        response.setId(
                resume.getId()
        );

        if (resume.getUser() != null) {

            response.setUserId(
                    resume.getUser().getId()
            );
        }

        response.setOriginalFileName(
                resume.getOriginalFileName()
        );

        response.setCloudinaryUrl(
                resume.getCloudinaryUrl()
        );

        response.setStatus(
                resume.getStatus()
        );

        response.setCreatedAt(
                resume.getCreatedAt()
        );

        response.setUpdatedAt(
                resume.getUpdatedAt()
        );

        // =====================================================
        // Parsed resume data
        // =====================================================

        ResumeParsedData parsedData =
                resume.getParsedData();

        if (parsedData != null
                && parsedData.getParsedJson() != null
                && !parsedData.getParsedJson().isBlank()) {

            try {

                ParsedResumeResponse parsedResume =
                        objectMapper.readValue(
                                parsedData.getParsedJson(),
                                ParsedResumeResponse.class
                        );

                response.setParsedData(
                        parsedResume
                );

            } catch (JacksonException e) {

                // IMPORTANT:
                // Print the real Jackson error to the console
                e.printStackTrace();

                throw new RuntimeException(
                        "Failed to parse stored resume JSON. " +
                                "Reason: " + e.getMessage(),
                        e
                );
            }
        }

        return response;
    }

    // =========================================================
    // GET MY RESUMES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<Resume> getResumesByUserEmail(
            String email
    ) {

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found with email: "
                                                + email
                                )
                        );

        return resumeRepository.findByUserId(
                user.getId()
        );
    }
}