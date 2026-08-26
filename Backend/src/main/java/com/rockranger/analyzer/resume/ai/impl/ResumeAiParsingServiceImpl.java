package com.rockranger.analyzer.resume.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockranger.analyzer.resume.ai.ResumeAiParsingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    @Autowired
    public ResumeAiParsingServiceImpl(
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.url}") String apiUrl,
            @Value("${groq.api.model}") String model
    ) {
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.restClient = RestClient.builder().build();

        log.info("Groq configuration loaded");
        log.info("Groq URL: {}", apiUrl);
        log.info("Groq model: {}", model);
        log.info(
                "Groq API key configured: {}",
                apiKey != null && !apiKey.isBlank()
        );
    }

    @Override
    public String parseResume(String extractedText) {

        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException(
                    "Resume extracted text cannot be empty."
            );
        }

        log.info(
                "Starting Groq resume parsing. Extracted text length: {}",
                extractedText.length()
        );

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

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "temperature", 0,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                ),
                "response_format", Map.of(
                        "type", "json_object"
                )
        );

        try {

            log.info("Sending request to Groq...");
            log.debug("Groq endpoint: {}", apiUrl);
            log.debug("Groq model: {}", model);

            String response = restClient.post()
                    .uri(apiUrl)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + apiKey
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.info("Groq request successful.");

            if (response == null || response.isBlank()) {
                throw new RuntimeException(
                        "Groq returned an empty HTTP response."
                );
            }

            log.debug("Raw Groq response: {}", response);

            JsonNode root = objectMapper.readTree(response);

            JsonNode content = root
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content");

            if (content.isMissingNode()
                    || content.isNull()
                    || content.asText().isBlank()) {

                log.error(
                        "Groq response does not contain choices[0].message.content"
                );

                throw new RuntimeException(
                        "Groq returned an empty AI content response."
                );
            }

            String json = content.asText();

            log.info(
                    "Groq returned parsed JSON. Length: {}",
                    json.length()
            );

            // Validate returned JSON
            objectMapper.readTree(json);

            log.info("Groq JSON validation successful.");

            return json;

        } catch (RestClientResponseException e) {

            log.error(
                    "Groq HTTP error. Status: {}, Response: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
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
}