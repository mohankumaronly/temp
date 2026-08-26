package com.rockranger.analyzer.resume.processing;

import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.resume.ai.ResumeAiParsingService;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeParsedData;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.extraction.ResumeTextExtractionService;
import com.rockranger.analyzer.resume.repository.ResumeParsedDataRepository;
import com.rockranger.analyzer.resume.repository.ResumeRepository;
import com.rockranger.analyzer.resume.storage.ResumeStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeProcessingServiceImplTest {

    @Mock
    private ResumeStorageService resumeStorageService;

    @Mock
    private ResumeTextExtractionService resumeTextExtractionService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ResumeAiParsingService resumeAiParsingService;

    @Mock
    private ResumeParsedDataRepository resumeParsedDataRepository;

    @InjectMocks
    private ResumeProcessingServiceImpl resumeProcessingService;

    private Resume resume;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);

        resume = new Resume();
        resume.setId(10L);
        resume.setUser(user);
        resume.setOriginalFileName("Mohan.pdf");
        resume.setCloudinaryUrl("https://res.cloudinary.com/demo/raw/upload/v1/resumes/Mohan.pdf");
        resume.setStatus(ResumeStatus.UPLOADED);
    }

    @Test
    void testProcessSuccess() {
        byte[] mockBytes = "pdf data".getBytes();
        String extractedText = "Mohan Kumar\nJava Developer";
        String mockJson = "{\"personalInfo\":{\"name\":\"Mohan Kumar\"}}";

        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(resumeStorageService.download(resume.getCloudinaryUrl())).thenReturn(mockBytes);
        when(resumeTextExtractionService.extractText(mockBytes)).thenReturn(extractedText);
        when(resumeAiParsingService.parseResume(extractedText)).thenReturn(mockJson);
        when(resumeParsedDataRepository.findByResumeId(10L)).thenReturn(Optional.empty());

        resumeProcessingService.process(10L);

        assertEquals(ResumeStatus.COMPLETED, resume.getStatus());
        assertEquals(extractedText, resume.getExtractedText());
        verify(resumeRepository, times(3)).save(resume);
        verify(resumeParsedDataRepository, times(1)).save(any(ResumeParsedData.class));
    }

    @Test
    void testProcessFailureSetsStatusFailed() {
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(resumeStorageService.download(resume.getCloudinaryUrl())).thenThrow(new RuntimeException("Cloudinary error"));

        assertThrows(RuntimeException.class, () -> resumeProcessingService.process(10L));

        assertEquals(ResumeStatus.FAILED, resume.getStatus());
        verify(resumeRepository, times(2)).save(resume);
    }

    @Test
    void testProcessUploadedResumes() {
        byte[] mockBytes = "pdf data".getBytes();
        String extractedText = "Mohan Kumar";
        String mockJson = "{\"personalInfo\":{\"name\":\"Mohan Kumar\"}}";

        when(resumeRepository.findByStatus(ResumeStatus.UPLOADED)).thenReturn(java.util.List.of(resume));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(resumeStorageService.download(resume.getCloudinaryUrl())).thenReturn(mockBytes);
        when(resumeTextExtractionService.extractText(mockBytes)).thenReturn(extractedText);
        when(resumeAiParsingService.parseResume(extractedText)).thenReturn(mockJson);
        when(resumeParsedDataRepository.findByResumeId(10L)).thenReturn(Optional.empty());

        java.util.List<Resume> processed = resumeProcessingService.processUploadedResumes();
        assertEquals(1, processed.size());
        assertEquals(ResumeStatus.COMPLETED, processed.get(0).getStatus());
        verify(resumeParsedDataRepository, times(1)).save(any(ResumeParsedData.class));
    }
}
