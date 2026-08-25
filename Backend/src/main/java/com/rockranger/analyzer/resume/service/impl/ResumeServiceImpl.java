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
    public void uploadResume(MultipartFile file, Long userId) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file cannot be empty.");
        }

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF resumes are supported.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found.")
                );

        String cloudinaryUrl = resumeStorageService.upload(file);

        Resume resume = new Resume();

        resume.setUser(user);
        resume.setOriginalFileName(file.getOriginalFilename());
        resume.setCloudinaryUrl(cloudinaryUrl);
        resume.setStatus(ResumeStatus.UPLOADED);

        resumeRepository.save(resume);
    }
}