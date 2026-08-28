package com.rockranger.analyzer.resume.ai.retry;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AiRetryPolicy {

    /*
     * Maximum number of retry attempts.
     *
     * Initial request
     *      ↓
     * Retry 1
     *      ↓
     * Retry 2
     *      ↓
     * Retry 3
     *
     * Total possible provider calls = 4
     */
    private final int maxRetries = 3;


    /**
     * Determines whether an AI request should be retried.
     *
     * Retryable HTTP statuses:
     *
     * 429 -> Too Many Requests
     * 500 -> Internal Server Error
     * 502 -> Bad Gateway
     * 503 -> Service Unavailable
     * 504 -> Gateway Timeout
     */
    public boolean shouldRetry(
            Exception exception,
            int attempt
    ) {

        /*
         * Do not retry after the maximum
         * number of retry attempts.
         */
        if (attempt >= maxRetries) {
            return false;
        }


        /*
         * Handle HTTP errors returned by
         * the AI provider.
         */
        if (exception instanceof RestClientResponseException responseException) {

            HttpStatus status =
                    HttpStatus.resolve(
                            responseException
                                    .getStatusCode()
                                    .value()
                    );


            /*
             * Unknown HTTP status.
             */
            if (status == null) {
                return false;
            }


            /*
             * 429 = Too Many Requests.
             *
             * This normally indicates:
             *
             * - rate limit
             * - token limit
             * - request limit
             */
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                return true;
            }


            /*
             * Temporary provider/server failures.
             */
            return status == HttpStatus.INTERNAL_SERVER_ERROR
                    || status == HttpStatus.BAD_GATEWAY
                    || status == HttpStatus.SERVICE_UNAVAILABLE
                    || status == HttpStatus.GATEWAY_TIMEOUT;
        }


        /*
         * Do not retry arbitrary application errors.
         *
         * Examples:
         *
         * 400 -> Bad Request
         * 401 -> Unauthorized
         * 403 -> Forbidden
         *
         * Retrying these normally will not help.
         */
        return false;
    }


    /**
     * Returns the fallback exponential backoff delay.
     *
     * Retry 1 -> 2 seconds
     * Retry 2 -> 4 seconds
     * Retry 3 -> 8 seconds
     *
     * This is currently the provider-independent
     * fallback retry strategy.
     */
    public long getBackoffDelay(
            int attempt
    ) {

        return switch (attempt) {

            case 1 -> 2_000L;

            case 2 -> 4_000L;

            case 3 -> 8_000L;

            default -> 8_000L;
        };
    }


    /**
     * Returns the maximum number of retries.
     */
    public int getMaxRetries() {

        return maxRetries;
    }
}