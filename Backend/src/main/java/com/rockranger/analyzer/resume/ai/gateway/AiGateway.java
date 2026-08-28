package com.rockranger.analyzer.resume.ai.gateway;

import com.rockranger.analyzer.resume.ai.limiter.AiRateLimiter;
import com.rockranger.analyzer.resume.ai.provider.AiProvider;
import com.rockranger.analyzer.resume.ai.retry.AiRetryPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

@Component
public class AiGateway {

    private static final Logger log =
            LoggerFactory.getLogger(AiGateway.class);


    private final AiProvider aiProvider;

    private final AiRateLimiter aiRateLimiter;

    private final AiRetryPolicy aiRetryPolicy;


    public AiGateway(
            AiProvider aiProvider,
            AiRateLimiter aiRateLimiter,
            AiRetryPolicy aiRetryPolicy
    ) {

        this.aiProvider =
                aiProvider;

        this.aiRateLimiter =
                aiRateLimiter;

        this.aiRetryPolicy =
                aiRetryPolicy;


        log.info(
                "AI Gateway initialized."
        );
    }


    /**
     * Central entry point for all AI requests.
     *
     * Responsibilities:
     *
     * 1. Validate prompts
     * 2. Estimate request tokens
     * 3. Apply local rate limiting
     * 4. Call AI provider
     * 5. Detect temporary provider failures
     * 6. Retry temporary failures
     * 7. Apply exponential backoff
     * 8. Return provider response
     */
    public String generate(
            String systemPrompt,
            String userPrompt
    ) {

        validatePrompt(
                systemPrompt,
                "System prompt"
        );

        validatePrompt(
                userPrompt,
                "User prompt"
        );


        // ============================================================
        // TOKEN ESTIMATION
        // ============================================================

        int estimatedTokens =
                estimateTokens(
                        systemPrompt,
                        userPrompt
                );


        log.debug(
                "Estimated AI request tokens: {}",
                estimatedTokens
        );


        // ============================================================
        // RETRY LOOP
        // ============================================================

        /*
         * attempt = 0 means the initial request.
         */
        int attempt = 0;


        while (true) {

            try {

                /*
                 * ----------------------------------------------------
                 * LOCAL RATE LIMITER
                 * ----------------------------------------------------
                 *
                 * The limiter is executed before EVERY provider
                 * request.
                 *
                 * Initial request -> limiter
                 * Retry request   -> limiter
                 */
                aiRateLimiter.acquire(
                        estimatedTokens
                );


                log.info(
                        "Sending AI request. Attempt: {}",
                        attempt + 1
                );


                /*
                 * ----------------------------------------------------
                 * AI PROVIDER
                 * ----------------------------------------------------
                 */
                String response =
                        aiProvider.generate(
                                systemPrompt,
                                userPrompt
                        );


                /*
                 * ----------------------------------------------------
                 * RESPONSE VALIDATION
                 * ----------------------------------------------------
                 */
                if (response == null
                        || response.isBlank()) {

                    throw new RuntimeException(
                            "AI provider returned an empty response."
                    );
                }


                log.info(
                        "AI request completed successfully."
                );


                return response;


            } catch (Exception e) {

                /*
                 * ----------------------------------------------------
                 * RETRY DECISION
                 * ----------------------------------------------------
                 */

                boolean shouldRetry =
                        aiRetryPolicy.shouldRetry(
                                e,
                                attempt
                        );


                /*
                 * No retry.
                 */
                if (!shouldRetry) {

                    log.error(
                            "AI request failed and will not be retried. " +
                                    "Attempt: {}",
                            attempt + 1,
                            e
                    );


                    /*
                     * Preserve the original exception.
                     */
                    throw e;
                }


                /*
                 * ----------------------------------------------------
                 * NEXT RETRY ATTEMPT
                 * ----------------------------------------------------
                 */

                attempt++;


                /*
                 * ----------------------------------------------------
                 * FALLBACK EXPONENTIAL BACKOFF
                 * ----------------------------------------------------
                 *
                 * Retry 1 -> 2 seconds
                 * Retry 2 -> 4 seconds
                 * Retry 3 -> 8 seconds
                 *
                 * This is currently provider-independent.
                 */
                long delay =
                        aiRetryPolicy.getBackoffDelay(
                                attempt
                        );


                log.warn(
                        "AI request failed. " +
                                "Retrying in {} ms. Attempt: {}/{}",
                        delay,
                        attempt,
                        aiRetryPolicy.getMaxRetries()
                );


                /*
                 * ----------------------------------------------------
                 * WAIT BEFORE RETRY
                 * ----------------------------------------------------
                 */

                sleep(
                        delay
                );
            }
        }
    }


    // ================================================================
    // TOKEN ESTIMATION
    // ================================================================

    /**
     * Estimates the number of input tokens.
     *
     * This is NOT an exact provider tokenizer.
     *
     * It is only used by the application-side
     * rate limiter.
     *
     * Conservative approximation:
     *
     * 1 token ≈ 3 characters
     *
     * A small safety margin is also added.
     */
    private int estimateTokens(
            String systemPrompt,
            String userPrompt
    ) {

        long totalCharacters =
                (long) systemPrompt.length()
                        + userPrompt.length();


        /*
         * Conservative approximation:
         *
         * 1 token ≈ 3 characters.
         */
        long estimatedTokens =
                Math.max(
                        1,
                        (totalCharacters + 2) / 3
                );


        /*
         * Add approximately 10% safety margin,
         * with a minimum of 100 tokens.
         */
        estimatedTokens =
                estimatedTokens
                        + Math.max(
                        100,
                        estimatedTokens / 10
                );


        /*
         * Since we ultimately return an int,
         * make sure the long value fits.
         */
        if (estimatedTokens > Integer.MAX_VALUE) {

            throw new IllegalArgumentException(
                    "AI request is too large."
            );
        }


        return (int) estimatedTokens;
    }


    // ================================================================
    // PROMPT VALIDATION
    // ================================================================

    private void validatePrompt(
            String prompt,
            String fieldName
    ) {

        if (prompt == null
                || prompt.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName +
                            " cannot be empty."
            );
        }
    }


    // ================================================================
    // RETRY SLEEP
    // ================================================================

    private void sleep(
            long milliseconds
    ) {

        if (milliseconds <= 0) {
            return;
        }


        try {

            Thread.sleep(
                    milliseconds
            );


        } catch (InterruptedException e) {

            /*
             * Restore interrupted status.
             */
            Thread.currentThread().interrupt();


            throw new RuntimeException(
                    "AI retry wait was interrupted.",
                    e
            );
        }
    }
}