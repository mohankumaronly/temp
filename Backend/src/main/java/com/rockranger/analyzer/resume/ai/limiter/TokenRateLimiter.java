package com.rockranger.analyzer.resume.ai.limiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenRateLimiter implements AiRateLimiter {

    private static final Logger log =
            LoggerFactory.getLogger(TokenRateLimiter.class);

    /*
     * Maximum estimated tokens allowed during
     * one rate-limit window.
     *
     * This should be configured according to
     * the currently selected AI provider/account.
     */
    private final long tokensPerMinute;

    /*
     * Start of the current rate-limit window.
     */
    private long windowStartMillis;

    /*
     * Estimated tokens already reserved
     * inside the current window.
     */
    private long usedTokens;


    public TokenRateLimiter(
            @Value("${ai.rate-limit.tokens-per-minute:8000}")
            long tokensPerMinute
    ) {

        if (tokensPerMinute <= 0) {

            throw new IllegalArgumentException(
                    "AI tokens-per-minute limit must be greater than zero."
            );
        }

        this.tokensPerMinute = tokensPerMinute;

        this.windowStartMillis =
                System.currentTimeMillis();

        this.usedTokens = 0;

        log.info(
                "AI token rate limiter initialized. TPM limit: {}",
                tokensPerMinute
        );
    }


    @Override
    public synchronized void acquire(
            int estimatedTokens
    ) {

        if (estimatedTokens <= 0) {

            throw new IllegalArgumentException(
                    "Estimated token count must be greater than zero."
            );
        }

        /*
         * If one individual request is larger than
         * the configured TPM limit, waiting will never
         * make that request fit into the window.
         */
        if (estimatedTokens > tokensPerMinute) {

            throw new IllegalArgumentException(
                    "Estimated request tokens (" +
                            estimatedTokens +
                            ") exceed the configured AI " +
                            "tokens-per-minute limit (" +
                            tokensPerMinute +
                            ")."
            );
        }


        while (true) {

            long now =
                    System.currentTimeMillis();


            long elapsed =
                    now - windowStartMillis;


            /*
             * Start a new one-minute window.
             */
            if (elapsed >= 60_000L) {

                windowStartMillis = now;

                usedTokens = 0;

                log.debug(
                        "AI token rate-limit window reset."
                );
            }


            /*
             * Check whether this request fits
             * inside the current window.
             */
            if (usedTokens + estimatedTokens
                    <= tokensPerMinute) {

                usedTokens += estimatedTokens;

                log.debug(
                        "AI request allowed. Estimated tokens: {}, " +
                                "reserved: {}/{}",
                        estimatedTokens,
                        usedTokens,
                        tokensPerMinute
                );

                return;
            }


            /*
             * Request does not fit.
             *
             * Wait until the current window expires.
             */
            long remainingMillis =
                    60_000L - elapsed;


            if (remainingMillis <= 0) {

                continue;
            }


            log.info(
                    "AI token limit reached. " +
                            "Waiting {} ms before next request.",
                    remainingMillis
            );


            try {

                wait(remainingMillis);

            } catch (InterruptedException e) {

                /*
                 * Restore the interrupted status.
                 */
                Thread.currentThread().interrupt();

                throw new RuntimeException(
                        "AI rate limiter wait was interrupted.",
                        e
                );
            }
        }
    }
}