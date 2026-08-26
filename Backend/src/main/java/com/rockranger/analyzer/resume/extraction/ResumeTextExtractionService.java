package com.rockranger.analyzer.resume.extraction;

public interface ResumeTextExtractionService {

    String extractText(byte[] fileBytes);
}