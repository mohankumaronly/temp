package com.rockranger.analyzer.resume.ai.limiter;

public interface AiRateLimiter {

    /**
     * Wait until the AI request is allowed to proceed.
     *
     * @param estimatedTokens estimated number of tokens
     */
    void acquire(int estimatedTokens);
}