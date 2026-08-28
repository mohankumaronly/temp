package com.rockranger.analyzer.resume.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockranger.analyzer.resume.ai.schema.ResumeAiJsonSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class GroqAiProvider implements AiProvider {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GroqAiProvider.class
            );

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String apiUrl;

    private final String apiKey;

    private final String model;


    public GroqAiProvider(
            @Value("${groq.api.url}") String apiUrl,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.model}") String model
    ) {

        this.restClient =
                RestClient.builder().build();

        this.objectMapper =
                new ObjectMapper();

        this.apiUrl =
                apiUrl;

        this.apiKey =
                apiKey;

        this.model =
                model;


        /*
         * Never log the API key.
         */
        log.info(
                "Groq AI provider initialized."
        );

        log.info(
                "Groq URL: {}",
                apiUrl
        );

        log.info(
                "Groq model: {}",
                model
        );
    }


    // ================================================================
    // GENERATE
    // ================================================================

    @Override
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
        // RESPONSE FORMAT
        // ============================================================

        /*
         * Groq Structured Outputs.
         *
         * strict=true is intentional.
         *
         * GPT-OSS 120B supports strict structured outputs.
         */

        Map<String, Object> responseFormat =
                Map.of(

                        "type",
                        "json_schema",

                        "json_schema",
                        Map.of(

                                "name",
                                "resume_extraction",

                                "strict",
                                true,

                                "schema",
                                ResumeAiJsonSchema.build()
                        )
                );


        // ============================================================
        // REQUEST BODY
        // ============================================================

        Map<String, Object> requestBody =
                Map.of(

                        "model",
                        model,


                        /*
                         * Deterministic extraction.
                         */
                        "temperature",
                        0,


                        /*
                         * GPT-OSS reasoning control.
                         *
                         * Resume extraction does not require
                         * heavy reasoning.
                         */
                        "reasoning_effort",
                        "low",


                        /*
                         * Structured JSON output.
                         */
                        "response_format",
                        responseFormat,


                        /*
                         * Streaming is not required.
                         */
                        "stream",
                        false,


                        /*
                         * Conversation messages.
                         */
                        "messages",
                        List.of(

                                Map.of(
                                        "role",
                                        "system",

                                        "content",
                                        systemPrompt
                                ),

                                Map.of(
                                        "role",
                                        "user",

                                        "content",
                                        userPrompt
                                )
                        )
                );


        // ============================================================
        // SEND REQUEST
        // ============================================================

        try {

            log.info(
                    "Sending resume parsing request to Groq. " +
                            "Model: {}",
                    model
            );


            String response =
                    restClient.post()

                            .uri(apiUrl)

                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )

                            .header(
                                    "Authorization",
                                    "Bearer " + apiKey
                            )

                            .body(requestBody)

                            .retrieve()

                            .body(String.class);


            // ========================================================
            // HTTP RESPONSE VALIDATION
            // ========================================================

            if (response == null
                    || response.isBlank()) {

                throw new RuntimeException(
                        "Groq returned an empty HTTP response."
                );
            }


            log.debug(
                    "Groq HTTP response received. Length: {}",
                    response.length()
            );


            // ========================================================
            // PARSE GROQ RESPONSE
            // ========================================================

            JsonNode root =
                    objectMapper.readTree(
                            response
                    );


            JsonNode choices =
                    root.path(
                            "choices"
                    );


            if (!choices.isArray()
                    || choices.isEmpty()) {

                log.error(
                        "Groq response does not contain choices."
                );

                throw new RuntimeException(
                        "Groq response does not contain choices."
                );
            }


            JsonNode firstChoice =
                    choices.get(0);


            JsonNode message =
                    firstChoice.path(
                            "message"
                    );


            if (message.isMissingNode()
                    || !message.isObject()) {

                throw new RuntimeException(
                        "Groq response does not contain " +
                                "a valid message."
                );
            }


            JsonNode content =
                    message.path(
                            "content"
                    );


            // ========================================================
            // CONTENT VALIDATION
            // ========================================================

            if (content.isMissingNode()
                    || content.isNull()
                    || content.asText().isBlank()) {

                log.error(
                        "Groq response does not contain " +
                                "valid message content."
                );

                throw new RuntimeException(
                        "Groq returned empty AI content."
                );
            }


            String generatedContent =
                    content.asText();


            log.info(
                    "Groq generation completed successfully. " +
                            "Response length: {}",
                    generatedContent.length()
            );


            // ========================================================
            // BASIC JSON VALIDATION
            // ========================================================

            validateJson(
                    generatedContent
            );


            return generatedContent;


        } catch (RestClientResponseException e) {

            /*
             * IMPORTANT:
             *
             * Preserve the original HTTP exception.
             *
             * AiRetryPolicy needs the actual status code,
             * especially 429.
             */

            log.error(
                    "Groq HTTP request failed. Status: {}",
                    e.getStatusCode()
            );


            throw e;


        } catch (Exception e) {

            log.error(
                    "Unexpected error while communicating " +
                            "with Groq.",
                    e
            );


            throw new RuntimeException(
                    "Failed to communicate with Groq AI provider.",
                    e
            );
        }
    }


    // ================================================================
    // JSON VALIDATION
    // ================================================================

    private void validateJson(
            String generatedContent
    ) {

        try {

            JsonNode json =
                    objectMapper.readTree(
                            generatedContent
                    );


            if (json == null
                    || !json.isObject()) {

                throw new RuntimeException(
                        "Groq returned JSON that is not an object."
                );
            }


        } catch (Exception e) {

            throw new RuntimeException(
                    "Groq returned invalid JSON.",
                    e
            );
        }
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
}