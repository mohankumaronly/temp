package com.rockranger.analyzer.resume.processing;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ResumeRetryPolicy {

    private static final int MAX_RETRIES = 3;

    /**
     * Determine whether a failed operation should be retried.
     *
     * Currently this is designed primarily for temporary
     * AI/API failures.
     */
    public boolean shouldRetry(
            Exception exception,
            int attempt
    ) {

        if (exception == null) {
            return false;
        }

        /*
         * attempt represents the current retry attempt.
         *
         * Once maximum retries are reached, stop.
         */
        if (attempt >= MAX_RETRIES) {
            return false;
        }

        if (exception instanceof RestClientResponseException responseException) {

            HttpStatus status =
                    HttpStatus.resolve(
                            responseException
                                    .getStatusCode()
                                    .value()
                    );

            if (status == null) {
                return false;
            }

            /*
             * Retry temporary failures:
             *
             * 429 -> rate limit
             * 500 -> server error
             * 502 -> bad gateway
             * 503 -> service unavailable
             * 504 -> gateway timeout
             */

            return status == HttpStatus.TOO_MANY_REQUESTS
                    || status == HttpStatus.INTERNAL_SERVER_ERROR
                    || status == HttpStatus.BAD_GATEWAY
                    || status == HttpStatus.SERVICE_UNAVAILABLE
                    || status == HttpStatus.GATEWAY_TIMEOUT;
        }

        /*
         * Do not retry arbitrary application errors.
         *
         * Examples:
         *
         * 400 -> bad request
         * 401 -> authentication problem
         * 403 -> forbidden
         */
        return false;
    }

    /**
     * Exponential backoff delay.
     */
    public long getBackoffDelay(int attempt) {

        return switch (attempt) {

            case 1 -> 2_000L;

            case 2 -> 4_000L;

            case 3 -> 8_000L;

            default -> 8_000L;
        };
    }

    public int getMaxRetries() {
        return MAX_RETRIES;
    }
}