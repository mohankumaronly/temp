package com.rockranger.analyzer.resume.service;

import com.rockranger.analyzer.authentication.entity.User;
import com.rockranger.analyzer.authentication.repository.UserRepository;
import com.rockranger.analyzer.resume.entity.Resume;
import com.rockranger.analyzer.resume.entity.ResumeStatus;
import com.rockranger.analyzer.resume.processing.ResumeProcessingService;
import com.rockranger.analyzer.resume.repository.ResumeRepository;
import com.rockranger.analyzer.resume.service.impl.ResumeServiceImpl;
import com.rockranger.analyzer.resume.storage.ResumeStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResumeStorageService resumeStorageService;

    @Mock
    private ResumeProcessingService resumeProcessingService;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
    }

    @Test
    void testUploadResumeSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Mohan.pdf",
                "application/pdf",
                "dummy pdf content".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(resumeStorageService.upload(file)).thenReturn("https://res.cloudinary.com/demo/raw/upload/v1/resumes/Mohan.pdf");
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(10L);
            return r;
        });

        when(resumeRepository.findById(10L)).thenAnswer(invocation -> {
            Resume r = new Resume();
            r.setId(10L);
            r.setUser(testUser);
            r.setOriginalFileName("Mohan.pdf");
            r.setCloudinaryUrl("https://res.cloudinary.com/demo/raw/upload/v1/resumes/Mohan.pdf");
            r.setStatus(ResumeStatus.COMPLETED);
            return Optional.of(r);
        });

        Resume result = resumeService.uploadResume(file, 1L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Mohan.pdf", result.getOriginalFileName());
        assertEquals("https://res.cloudinary.com/demo/raw/upload/v1/resumes/Mohan.pdf", result.getCloudinaryUrl());
        assertEquals(ResumeStatus.COMPLETED, result.getStatus());
        verify(resumeProcessingService, times(1)).process(10L);
    }

    @Test
    void testUploadNonPdfThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "dummy content".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> resumeService.uploadResume(file, 1L));
    }
}
