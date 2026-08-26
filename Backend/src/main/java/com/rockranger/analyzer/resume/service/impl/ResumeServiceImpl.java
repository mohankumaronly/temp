package com.rockranger.analyzer.resume.service.impl;

import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.authentication.repository.UserRepository;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.repository.ResumeRepository;
import com.rockranger.analyzer.resume.service.ResumeService;
import com.rockranger.analyzer.resume.storage.ResumeStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeStorageService resumeStorageService;

    public ResumeServiceImpl(
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            ResumeStorageService resumeStorageService
    ) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.resumeStorageService = resumeStorageService;
    }

    @Override
    @Transactional
    public java.util.List<Resume> uploadResumes(java.util.List<MultipartFile> files, String email) {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one resume is required.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found.")
                );

        java.util.List<Resume> uploadedResumes = new java.util.ArrayList<>();

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Resume file cannot be empty.");
            }

            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            boolean isPdfByExtension = originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf");
            boolean isPdfByContentType = contentType != null && (contentType.equalsIgnoreCase("application/pdf") || contentType.equalsIgnoreCase("application/x-pdf"));

            if (!isPdfByExtension && !isPdfByContentType) {
                throw new IllegalArgumentException("Only PDF resumes are supported: " + originalFilename);
            }

            // 1. Upload PDF to Cloudinary
            String cloudinaryUrl = resumeStorageService.upload(file);

            // 2. Create Resume record
            Resume resume = new Resume();

            resume.setUser(user);
            resume.setOriginalFileName(originalFilename != null ? originalFilename : "resume.pdf");
            resume.setCloudinaryUrl(cloudinaryUrl);
            resume.setStatus(ResumeStatus.UPLOADED);

            // 3. Save resume metadata in MySQL
            resume = resumeRepository.save(resume);

            uploadedResumes.add(resume);
        }

        return uploadedResumes;
    }

    @Override
    @Transactional(readOnly = true)
    public Resume getResumeById(Long id, String email) {
        return resumeRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new RuntimeException("Resume not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Resume> getResumesByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return resumeRepository.findByUserId(user.getId());
    }
}