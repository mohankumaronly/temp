package com.rockranger.analyzer.resume.processing;

import com.rockranger.analyzer.resume.entity.Resume;

import java.util.List;

public interface ResumeProcessingService {

    void process(Resume resume);

    void process(Long resumeId);

    List<Resume> processUploadedResumes();
}