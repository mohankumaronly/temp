package com.rockranger.analyzer.resume.processing;

import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.extraction.ResumeTextExtractionService;
import com.rockranger.analyzer.resume.repository.ResumeRepository;
import com.rockranger.analyzer.resume.storage.ResumeStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeProcessingServiceImplTest {

    @Mock
    private ResumeStorageService resumeStorageService;

    @Mock
    private ResumeTextExtractionService resumeTextExtractionService;

    @Mock
    private ResumeRepository resumeRepository;

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
        when(resumeRepository.findById(10L)).thenReturn(java.util.Optional.of(resume));
        when(resumeStorageService.download(resume.getCloudinaryUrl())).thenReturn(mockBytes);
        when(resumeTextExtractionService.extractText(mockBytes)).thenReturn("Mohan Kumar\nJava Developer");

        resumeProcessingService.process(10L);

        assertEquals(ResumeStatus.COMPLETED, resume.getStatus());
        assertEquals("Mohan Kumar\nJava Developer", resume.getExtractedText());
        verify(resumeRepository, times(2)).save(resume);
    }

    @Test
    void testProcessFailureSetsStatusFailed() {
        when(resumeRepository.findById(10L)).thenReturn(java.util.Optional.of(resume));
        when(resumeStorageService.download(resume.getCloudinaryUrl())).thenThrow(new RuntimeException("Cloudinary error"));

        assertThrows(RuntimeException.class, () -> resumeProcessingService.process(10L));

        assertEquals(ResumeStatus.FAILED, resume.getStatus());
        verify(resumeRepository, times(2)).save(resume);
    }

    @Test
    void testProcessUploadedResumes() {
        when(resumeRepository.findByStatus(ResumeStatus.UPLOADED)).thenReturn(java.util.List.of(resume));
        when(resumeRepository.findById(10L)).thenReturn(java.util.Optional.of(resume));
        byte[] mockBytes = "pdf data".getBytes();
        when(resumeStorageService.download(resume.getCloudinaryUrl())).thenReturn(mockBytes);
        when(resumeTextExtractionService.extractText(mockBytes)).thenReturn("Mohan Kumar");

        java.util.List<Resume> processed = resumeProcessingService.processUploadedResumes();
        assertEquals(1, processed.size());
        assertEquals(ResumeStatus.COMPLETED, processed.get(0).getStatus());
    }
}
