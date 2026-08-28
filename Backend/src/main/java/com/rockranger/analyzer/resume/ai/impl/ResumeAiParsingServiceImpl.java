package com.rockranger.analyzer.resume.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rockranger.analyzer.resume.ai.ResumeAiParsingService;
import com.rockranger.analyzer.resume.ai.gateway.AiGateway;
import com.rockranger.analyzer.resume.ai.prompt.ResumeAiPromptProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Iterator;

@Service
public class ResumeAiParsingServiceImpl
        implements ResumeAiParsingService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ResumeAiParsingServiceImpl.class
            );

    private final ObjectMapper objectMapper;
    private final ResumeAiPromptProvider promptProvider;
    private final AiGateway aiGateway;


    public ResumeAiParsingServiceImpl(
            ResumeAiPromptProvider promptProvider,
            AiGateway aiGateway
    ) {

        /*
         * Create ObjectMapper locally.
         *
         * This avoids requiring ObjectMapper to be
         * registered as a Spring bean.
         */
        this.objectMapper =
                new ObjectMapper();

        this.promptProvider =
                promptProvider;

        this.aiGateway =
                aiGateway;

        log.info(
                "Resume AI parsing service initialized."
        );
    }


    // ================================================================
    // PARSE RESUME
    // ================================================================

    @Override
    public String parseResume(
            String extractedText
    ) {

        // ============================================================
        // 1. INPUT VALIDATION
        // ============================================================

        if (extractedText == null
                || extractedText.isBlank()) {

            throw new IllegalArgumentException(
                    "Resume extracted text cannot be empty."
            );
        }


        log.info(
                "Starting resume AI parsing. " +
                        "Extracted text length: {}",
                extractedText.length()
        );


        // ============================================================
        // 2. BUILD SYSTEM PROMPT
        // ============================================================

        String systemPrompt =
                promptProvider.getSystemPrompt();


        if (systemPrompt == null
                || systemPrompt.isBlank()) {

            throw new IllegalStateException(
                    "Resume AI system prompt cannot be empty."
            );
        }


        // ============================================================
        // 3. BUILD USER PROMPT
        // ============================================================

        String userPrompt =
                promptProvider.buildUserPrompt(
                        extractedText
                );


        if (userPrompt == null
                || userPrompt.isBlank()) {

            throw new IllegalStateException(
                    "Resume AI user prompt cannot be empty."
            );
        }


        log.debug(
                "Resume AI prompts prepared."
        );


        // ============================================================
        // 4. SEND REQUEST THROUGH AI GATEWAY
        // ============================================================

        /*
         * IMPORTANT:
         *
         * This class does NOT communicate directly with Groq.
         *
         * AiGateway handles:
         *
         * - token estimation
         * - rate limiting
         * - retry handling
         * - AI provider communication
         *
         * Flow:
         *
         * ResumeAiParsingServiceImpl
         *              ↓
         *          AiGateway
         *              ↓
         *       AiRateLimiter
         *              ↓
         *        AiProvider
         *              ↓
         *       GroqAiProvider
         */

        String json;

        try {

            log.info(
                    "Sending resume to AI Gateway."
            );


            json =
                    aiGateway.generate(
                            systemPrompt,
                            userPrompt
                    );


        } catch (Exception e) {

            /*
             * Do not retry here.
             *
             * AiGateway owns the retry mechanism.
             */
            log.error(
                    "AI Gateway failed while parsing resume.",
                    e
            );


            throw new RuntimeException(
                    "Resume AI parsing failed.",
                    e
            );
        }


        // ============================================================
        // 5. RESPONSE VALIDATION
        // ============================================================

        if (json == null
                || json.isBlank()) {

            log.error(
                    "AI Gateway returned an empty response."
            );


            throw new RuntimeException(
                    "AI returned an empty response."
            );
        }


        log.info(
                "AI Gateway returned response. " +
                        "JSON length: {}",
                json.length()
        );


        // ============================================================
        // 6. JSON SYNTAX VALIDATION
        // ============================================================

        JsonNode parsedJson;

        try {

            parsedJson =
                    objectMapper.readTree(
                            json
                    );


        } catch (Exception e) {

            log.error(
                    "AI returned invalid JSON.",
                    e
            );


            throw new RuntimeException(
                    "AI returned invalid JSON.",
                    e
            );
        }


        if (parsedJson == null
                || !parsedJson.isObject()) {

            throw new RuntimeException(
                    "AI response must be a JSON object."
            );
        }


        log.debug(
                "AI JSON syntax validation successful."
        );


        // ============================================================
        // 7. APPLICATION STRUCTURE VALIDATION
        // ============================================================

        validateTopLevelStructure(
                parsedJson
        );


        log.info(
                "Resume AI JSON validation successful."
        );


        // ============================================================
        // 8. RETURN VALIDATED JSON
        // ============================================================

        return json;
    }


    // ================================================================
    // TOP LEVEL VALIDATION
    // ================================================================

    private void validateTopLevelStructure(
            JsonNode json
    ) {

        /*
         * These are the only allowed top-level
         * properties in the resume JSON.
         */

        String[] requiredFields = {

                "personalInfo",
                "summary",
                "skills",
                "experience",
                "education",
                "projects",
                "certifications"
        };


        // ============================================================
        // REQUIRED TOP LEVEL FIELDS
        // ============================================================

        for (String field :
                requiredFields) {

            if (!json.has(field)) {

                throw new RuntimeException(
                        "AI response is missing required field: "
                                + field
                );
            }
        }


        // ============================================================
        // PERSONAL INFORMATION
        // ============================================================

        JsonNode personalInfo =
                json.path(
                        "personalInfo"
                );


        if (!personalInfo.isObject()) {

            throw new RuntimeException(
                    "AI response field 'personalInfo' " +
                            "must be a JSON object."
            );
        }


        String[] personalInfoFields = {

                "name",
                "email",
                "phone",
                "location",
                "linkedin",
                "github",
                "portfolio"
        };


        for (String field :
                personalInfoFields) {

            if (!personalInfo.has(field)) {

                throw new RuntimeException(
                        "AI response is missing " +
                                "personalInfo field: "
                                + field
                );
            }
        }


        // ============================================================
        // SUMMARY
        // ============================================================

        /*
         * Summary is allowed to be null.
         *
         * We only require the property to exist.
         */

        if (!json.has("summary")) {

            throw new RuntimeException(
                    "AI response is missing 'summary'."
            );
        }


        // ============================================================
        // ARRAY FIELDS
        // ============================================================

        validateArrayField(
                json,
                "skills"
        );

        validateArrayField(
                json,
                "experience"
        );

        validateArrayField(
                json,
                "education"
        );

        validateArrayField(
                json,
                "projects"
        );

        validateArrayField(
                json,
                "certifications"
        );


        // ============================================================
        // UNEXPECTED TOP LEVEL FIELDS
        // ============================================================

        /*
         * Do not allow the AI to add fields such as:
         *
         * awards
         * achievements
         * references
         * hobbies
         * publications
         *
         * unless they are explicitly added to the schema later.
         */

        Iterator<String> fields =
                json.fieldNames();


        while (fields.hasNext()) {

            String actualField =
                    fields.next();


            boolean allowed =
                    false;


            for (String allowedField :
                    requiredFields) {

                if (allowedField.equals(
                        actualField
                )) {

                    allowed = true;

                    break;
                }
            }


            if (!allowed) {

                throw new RuntimeException(
                        "AI response contains unexpected " +
                                "top-level field: "
                                + actualField
                );
            }
        }
    }


    // ================================================================
    // ARRAY VALIDATION
    // ================================================================

    private void validateArrayField(
            JsonNode json,
            String fieldName
    ) {

        JsonNode field =
                json.path(
                        fieldName
                );


        if (!field.isArray()) {

            throw new RuntimeException(
                    "AI response field '" +
                            fieldName +
                            "' must be a JSON array."
            );
        }
    }
}