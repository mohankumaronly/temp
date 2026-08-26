package com.rockranger.analyzer.resume.service;

import com.rockranger.analyzer.resume.entity.Resume;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    Resume uploadResume(MultipartFile file, Long userId);

    Resume getResumeById(Long id);

    List<Resume> getResumesByUserId(Long userId);
}