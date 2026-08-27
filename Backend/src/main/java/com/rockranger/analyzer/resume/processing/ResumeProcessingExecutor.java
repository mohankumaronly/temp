package com.rockranger.analyzer.resume.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ResumeProcessingExecutor {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ResumeProcessingExecutor.class
            );

    private final ExecutorService executorService;
    private final ResumeProcessingService processingService;

    public ResumeProcessingExecutor(
            @Value("${resume.processing.worker-count:1}")
            int workerCount,
            ResumeProcessingService processingService
    ) {

        if (workerCount < 1) {
            throw new IllegalArgumentException(
                    "Resume worker count must be at least 1."
            );
        }

        this.processingService = processingService;

        this.executorService =
                Executors.newFixedThreadPool(workerCount);

        log.info(
                "Resume processing executor initialized with {} worker(s)",
                workerCount
        );
    }

    /**
     * Submit a resume for background processing.
     */
    public void submit(Long resumeId) {

        if (resumeId == null) {
            throw new IllegalArgumentException(
                    "Resume ID cannot be null."
            );
        }

        log.info(
                "Submitting Resume ID {} to processing executor",
                resumeId
        );

        executorService.submit(() -> {

            log.info(
                    "Background processing started for Resume ID: {}",
                    resumeId
            );

            try {

                processingService.process(resumeId);

                log.info(
                        "Background processing completed for Resume ID: {}",
                        resumeId
                );

            } catch (Exception e) {

                /*
                 * ResumeProcessingServiceImpl is responsible
                 * for marking the resume as FAILED.
                 *
                 * Executor only records the background task failure.
                 */

                log.error(
                        "Background processing failed for Resume ID: {}",
                        resumeId,
                        e
                );
            }
        });
    }

    /**
     * Gracefully shut down the executor when the application stops.
     */
    public void shutdown() {

        log.info(
                "Shutting down resume processing executor"
        );

        executorService.shutdown();

        log.info(
                "Resume processing executor shutdown initiated"
        );
    }
}