package com.rockranger.analyzer.resume.extraction;

public interface ResumeTextExtractor {

    String extractText(byte[] fileBytes);
}