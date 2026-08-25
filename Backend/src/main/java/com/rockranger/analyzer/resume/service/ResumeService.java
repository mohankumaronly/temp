package com.rockranger.analyzer.resume.service;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {

    void uploadResume(MultipartFile file, Long userId);

}