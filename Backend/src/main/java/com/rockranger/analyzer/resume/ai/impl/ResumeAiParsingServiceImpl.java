package com.rockranger.analyzer.resume.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockranger.analyzer.resume.ai.ResumeAiParsingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class ResumeAiParsingServiceImpl implements ResumeAiParsingService {

    private static final Logger log =
            LoggerFactory.getLogger(ResumeAiParsingServiceImpl.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final String apiUrl;
    private final String model;

    @Autowired
    public ResumeAiParsingServiceImpl(
            @Value("${ollama.api.url}") String apiUrl,
            @Value("${ollama.api.model}") String model
    ) {

        this.objectMapper = new ObjectMapper();

        this.apiUrl = apiUrl;
        this.model = model;

        this.restClient = RestClient.builder().build();

        // Production:
        // Log configuration information, but NEVER log API keys,
        // resume text, personal information, or complete AI responses.
        log.info("Ollama configuration loaded");
        log.info("Ollama URL: {}", apiUrl);
        log.info("Ollama model: {}", model);
    }

    @Override
    public String parseResume(String extractedText) {

        // Validate input before sending anything to the AI model.
        if (extractedText == null || extractedText.isBlank()) {

            throw new IllegalArgumentException(
                    "Resume extracted text cannot be empty."
            );
        }

        /*
         * Production:
         *
         * Do not log the actual resume text.
         * Resume data contains personal information.
         *
         * Logging only the length is safe and useful for debugging.
         */
        log.info(
                "Starting resume AI parsing. Extracted text length: {}",
                extractedText.length()
        );

        /*
         * System prompt defines the contract between our application
         * and the AI model.
         *
         * Production:
         * Keep this prompt centralized/versioned if it becomes large.
         */
        String systemPrompt = """
                You are a resume parsing system.

                Your task is to extract structured information
                from the provided resume text.

                Rules:
                1. Extract only information actually present in the resume.
                2. Never invent information.
                3. If information is missing, use null or an empty array.
                4. Return valid JSON only.
                5. Follow the requested JSON structure exactly.

                Return this structure:

                {
                  "personalInfo": {
                    "name": null,
                    "email": null,
                    "phone": null,
                    "location": null,
                    "linkedin": null,
                    "github": null,
                    "portfolio": null
                  },
                  "summary": null,
                  "skills": [],
                  "experience": [],
                  "education": [],
                  "projects": [],
                  "certifications": []
                }
                """;

        String userPrompt = """
                Parse the following resume.

                RESUME TEXT:

                %s
                """.formatted(extractedText);

        /*
         * Ollama /api/chat request.
         *
         * Unlike Groq/OpenAI-compatible APIs, Ollama does not require
         * an Authorization header for a local server.
         */
        Map<String, Object> requestBody = Map.of(

                "model", model,

                /*
                 * Production:
                 * temperature = 0 makes structured extraction
                 * more deterministic.
                 */
                "temperature", 0,

                /*
                 * Ask Ollama to return JSON.
                 */
                "format", "json",

                /*
                 * Prevent unnecessary conversation history.
                 */
                "stream", false,

                "messages", List.of(

                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),

                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                )
        );

        try {

            log.info(
                    "Sending resume to Ollama. Model: {}",
                    model
            );

            log.debug(
                    "Ollama endpoint: {}",
                    apiUrl
            );

            /*
             * Call local Ollama server.
             *
             * apiUrl should be:
             *
             * http://localhost:11434/api/chat
             */
            String response = restClient.post()

                    .uri(apiUrl)

                    .contentType(MediaType.APPLICATION_JSON)

                    .body(requestBody)

                    .retrieve()

                    .body(String.class);

            log.info("Ollama request completed successfully.");

            /*
             * Validate HTTP response body.
             */
            if (response == null || response.isBlank()) {

                throw new RuntimeException(
                        "Ollama returned an empty HTTP response."
                );
            }

            /*
             * IMPORTANT:
             *
             * Do not log the entire response in production.
             *
             * It may contain:
             * - name
             * - email
             * - phone
             * - experience
             * - resume information
             *
             * Therefore we only log the response size.
             */
            log.debug(
                    "Ollama response length: {}",
                    response.length()
            );

            /*
             * Parse Ollama response.
             *
             * Expected structure:
             *
             * {
             *   "message": {
             *      "role": "assistant",
             *      "content": "{ ...JSON... }"
             *   }
             * }
             */
            JsonNode root =
                    objectMapper.readTree(response);

            JsonNode content =
                    root.path("message")
                            .path("content");

            /*
             * Validate AI response.
             */
            if (content.isMissingNode()
                    || content.isNull()
                    || content.asText().isBlank()) {

                log.error(
                        "Ollama response does not contain message.content"
                );

                throw new RuntimeException(
                        "Ollama returned an empty AI content response."
                );
            }

            String json = content.asText();

            log.info(
                    "Ollama returned parsed JSON. Length: {}",
                    json.length()
            );

            /*
             * VERY IMPORTANT:
             *
             * Never directly trust AI output.
             *
             * Validate that the returned content is actually
             * valid JSON before storing it in MySQL.
             */
            objectMapper.readTree(json);

            log.info(
                    "Ollama JSON validation successful."
            );

            return json;

        } catch (RestClientResponseException e) {

            /*
             * Production:
             *
             * Log HTTP status and a safe error message.
             * Avoid logging sensitive request data.
             */
            log.error(
                    "Ollama HTTP error. Status: {}",
                    e.getStatusCode(),
                    e
            );

            throw new RuntimeException(
                    "Ollama API request failed. HTTP status: "
                            + e.getStatusCode(),
                    e
            );

        } catch (Exception e) {

            /*
             * Catch parsing/connection/JSON errors.
             */
            log.error(
                    "Unexpected error while parsing resume with Ollama AI",
                    e
            );

            throw new RuntimeException(
                    "Failed to parse resume using Ollama AI.",
                    e
            );
        }
    }
}