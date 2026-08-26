package com.rockranger.analyzer.resume.service.impl;

import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.authentication.repository.UserRepository;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.processing.ResumeProcessingService;
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
    private final ResumeProcessingService resumeProcessingService;

    public ResumeServiceImpl(
            ResumeRepository resumeRepository,
            UserRepository userRepository,
            ResumeStorageService resumeStorageService,
            ResumeProcessingService resumeProcessingService
    ) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.resumeStorageService = resumeStorageService;
        this.resumeProcessingService = resumeProcessingService;
    }

    @Override
    public Resume uploadResume(MultipartFile file, Long userId) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file cannot be empty.");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        boolean isPdfByExtension = originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf");
        boolean isPdfByContentType = contentType != null && (contentType.equalsIgnoreCase("application/pdf") || contentType.equalsIgnoreCase("application/x-pdf"));

        if (!isPdfByExtension && !isPdfByContentType) {
            throw new IllegalArgumentException("Only PDF resumes are supported.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with id: " + userId)
                );

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

        // 4. Download from Cloudinary → PDFBox → extract text → MySQL
        resumeProcessingService.process(resume.getId());

        // 5. Fetch and return latest state from MySQL
        return resumeRepository.findById(resume.getId())
                .orElse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public Resume getResumeById(Long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resume not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<Resume> getResumesByUserId(Long userId) {
        return resumeRepository.findByUserId(userId);
    }
}