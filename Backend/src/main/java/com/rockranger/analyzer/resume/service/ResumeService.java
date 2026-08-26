package com.rockranger.analyzer.resume.service;

import com.rockranger.analyzer.resume.entity.Resume;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    List<Resume> uploadResumes(List<MultipartFile> files, String email);

    Resume getResumeById(Long id, String email);

    List<Resume> getResumesByUserEmail(String email);
}