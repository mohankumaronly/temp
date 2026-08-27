package com.rockranger.analyzer.resume.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockranger.analyzer.resume.ai.ResumeAiParsingService;
import com.rockranger.analyzer.resume.ai.prompt.ResumeAiPromptProvider;
import com.rockranger.analyzer.resume.ai.schema.ResumeAiJsonSchema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class ResumeAiParsingServiceImpl
        implements ResumeAiParsingService {

    private static final Logger log =
            LoggerFactory.getLogger(ResumeAiParsingServiceImpl.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ResumeAiPromptProvider promptProvider;

    private final String apiKey;
    private final String apiUrl;
    private final String model;


    public ResumeAiParsingServiceImpl(
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.url}") String apiUrl,
            @Value("${groq.api.model}") String model,
            ResumeAiPromptProvider promptProvider
    ) {

        this.objectMapper = new ObjectMapper();

        this.restClient = RestClient.builder().build();

        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;

        this.promptProvider = promptProvider;

        log.info("Groq AI configuration loaded");
        log.info("Groq URL: {}", apiUrl);
        log.info("Groq model: {}", model);

        // Never log the actual API key.
        log.info(
                "Groq API key configured: {}",
                apiKey != null && !apiKey.isBlank()
        );
    }


    @Override
    public String parseResume(String extractedText) {

        // ================================================================
        // INPUT VALIDATION
        // ================================================================

        if (extractedText == null || extractedText.isBlank()) {

            throw new IllegalArgumentException(
                    "Resume extracted text cannot be empty."
            );
        }


        log.info(
                "Starting resume AI parsing. Extracted text length: {}",
                extractedText.length()
        );


        // ================================================================
        // BUILD PROMPTS
        // ================================================================

        String systemPrompt =
                promptProvider.getSystemPrompt();

        String userPrompt =
                promptProvider.buildUserPrompt(extractedText);


        // ================================================================
        // GROQ REQUEST
        // ================================================================

        Map<String, Object> requestBody = Map.of(

                "model",
                model,

                "temperature",
                0,

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
                ),

                "response_format",
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
                )
        );


        // ================================================================
        // CALL GROQ
        // ================================================================

        try {

            log.info(
                    "Sending resume to Groq. Model: {}",
                    model
            );

            log.debug(
                    "Groq endpoint: {}",
                    apiUrl
            );


            String response = restClient.post()

                    .uri(apiUrl)

                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + apiKey
                    )

                    .contentType(
                            MediaType.APPLICATION_JSON
                    )

                    .body(requestBody)

                    .retrieve()

                    .body(String.class);


            // ============================================================
            // HTTP RESPONSE VALIDATION
            // ============================================================

            if (response == null || response.isBlank()) {

                throw new RuntimeException(
                        "Groq returned an empty HTTP response."
                );
            }


            log.info(
                    "Groq request completed successfully."
            );

            log.debug(
                    "Groq response length: {}",
                    response.length()
            );


            // ============================================================
            // PARSE GROQ RESPONSE
            // ============================================================

            JsonNode root =
                    objectMapper.readTree(response);


            JsonNode content =
                    root.path("choices")
                            .path(0)
                            .path("message")
                            .path("content");


            if (content.isMissingNode()
                    || content.isNull()
                    || content.asText().isBlank()) {

                log.error(
                        "Groq response does not contain " +
                                "choices[0].message.content"
                );

                throw new RuntimeException(
                        "Groq returned an empty AI content response."
                );
            }


            String json =
                    content.asText();


            log.info(
                    "Groq returned structured resume JSON. Length: {}",
                    json.length()
            );


            // ============================================================
            // JSON SYNTAX VALIDATION
            // ============================================================

            JsonNode parsedJson =
                    objectMapper.readTree(json);


            if (parsedJson == null
                    || !parsedJson.isObject()) {

                throw new RuntimeException(
                        "Groq returned JSON that is not a JSON object."
                );
            }


            // ============================================================
            // APPLICATION VALIDATION
            // ============================================================

            validateTopLevelStructure(parsedJson);


            log.info(
                    "Groq JSON validation successful."
            );


            return json;


        } catch (RestClientResponseException e) {

            log.error(
                    "Groq HTTP error. Status: {}",
                    e.getStatusCode(),
                    e
            );


            throw new RuntimeException(
                    "Groq API request failed. HTTP status: "
                            + e.getStatusCode(),
                    e
            );


        } catch (Exception e) {

            log.error(
                    "Unexpected error while parsing resume with Groq AI",
                    e
            );


            throw new RuntimeException(
                    "Failed to parse resume using Groq AI.",
                    e
            );
        }
    }


    // ================================================================
    // APPLICATION-LEVEL VALIDATION
    // ================================================================

    private void validateTopLevelStructure(
            JsonNode json
    ) {

        String[] requiredFields = {

                "personalInfo",
                "summary",
                "skills",
                "experience",
                "education",
                "projects",
                "certifications"
        };


        for (String field : requiredFields) {

            if (!json.has(field)) {

                throw new RuntimeException(
                        "AI response is missing required field: "
                                + field
                );
            }
        }


        // personalInfo must be an object.

        if (!json.path("personalInfo").isObject()) {

            throw new RuntimeException(
                    "AI response field 'personalInfo' " +
                            "must be an object."
            );
        }


        // skills must be an array.

        if (!json.path("skills").isArray()) {

            throw new RuntimeException(
                    "AI response field 'skills' " +
                            "must be an array."
            );
        }


        // experience must be an array.

        if (!json.path("experience").isArray()) {

            throw new RuntimeException(
                    "AI response field 'experience' " +
                            "must be an array."
            );
        }


        // education must be an array.

        if (!json.path("education").isArray()) {

            throw new RuntimeException(
                    "AI response field 'education' " +
                            "must be an array."
            );
        }


        // projects must be an array.

        if (!json.path("projects").isArray()) {

            throw new RuntimeException(
                    "AI response field 'projects' " +
                            "must be an array."
            );
        }


        // certifications must be an array.

        if (!json.path("certifications").isArray()) {

            throw new RuntimeException(
                    "AI response field 'certifications' " +
                            "must be an array."
            );
        }
    }
}