package com.rockranger.analyzer.resume.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeStorageService {

    String upload(MultipartFile file);
}