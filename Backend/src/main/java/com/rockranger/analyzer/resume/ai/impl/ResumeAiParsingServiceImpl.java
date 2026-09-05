package com.rockranger.analyzer.resume.ai.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.rockranger.analyzer.resume.ai.ResumeAiParsingService;
import com.rockranger.analyzer.resume.ai.gateway.AiGateway;
import com.rockranger.analyzer.resume.ai.prompt.ResumeAiPromptProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

        String[] requiredFields = {

                "personalInfo",
                "summary",
                "skills",
                "experience",
                "education",
                "projects",
                "certifications"
        };

        Set<String> allowedTopLevelFields =
                new HashSet<>(
                        Set.of(requiredFields)
                );

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
        // TOP LEVEL FIELD TYPES
        // ============================================================

        JsonNode personalInfo =
                json.path("personalInfo");

        if (!personalInfo.isObject()) {

            throw new RuntimeException(
                    "AI response field 'personalInfo' " +
                            "must be a JSON object."
            );
        }

        JsonNode summary =
                json.path("summary");

        validateNullableString(
                summary,
                "summary"
        );

        validateStringArray(
                json.path("skills"),
                "skills"
        );

        validateObjectArray(
                json.path("experience"),
                "experience"
        );

        validateObjectArray(
                json.path("education"),
                "education"
        );

        validateObjectArray(
                json.path("projects"),
                "projects"
        );

        validateObjectArray(
                json.path("certifications"),
                "certifications"
        );

        // ============================================================
        // PERSONAL INFORMATION
        // ============================================================

        validatePersonalInfo(
                personalInfo
        );

        // ============================================================
        // EXPERIENCE
        // ============================================================

        validateExperience(
                json.path("experience")
        );

        // ============================================================
        // EDUCATION
        // ============================================================

        validateEducation(
                json.path("education")
        );

        // ============================================================
        // PROJECTS
        // ============================================================

        validateProjects(
                json.path("projects")
        );

        // ============================================================
        // CERTIFICATIONS
        // ============================================================

        validateCertifications(
                json.path("certifications")
        );

        // ============================================================
        // UNEXPECTED TOP LEVEL FIELDS
        // ============================================================

        for (Map.Entry<String, JsonNode> entry :
                json.properties()) {

            String actualField =
                    entry.getKey();

            if (!allowedTopLevelFields.contains(
                    actualField
            )) {

                throw new RuntimeException(
                        "AI response contains unexpected " +
                                "top-level field: "
                                + actualField
                );
            }
        }
    }

    // ================================================================
    // PERSONAL INFORMATION VALIDATION
    // ================================================================

    private void validatePersonalInfo(
            JsonNode personalInfo
    ) {

        String[] fields = {

                "name",
                "email",
                "phone",
                "location",
                "linkedin",
                "github",
                "portfolio"
        };

        Set<String> allowedFields =
                new HashSet<>(
                        Set.of(fields)
                );

        for (String field : fields) {

            if (!personalInfo.has(field)) {

                throw new RuntimeException(
                        "AI response is missing " +
                                "personalInfo field: "
                                + field
                );
            }

            validateNullableString(
                    personalInfo.path(field),
                    "personalInfo." + field
            );
        }

        validateUnexpectedFields(
                personalInfo,
                allowedFields,
                "personalInfo"
        );
    }

    // ================================================================
    // EXPERIENCE VALIDATION
    // ================================================================

    private void validateExperience(
            JsonNode experience
    ) {

        String[] fields = {

                "company",
                "role",
                "startDate",
                "endDate",
                "location",
                "responsibilities"
        };

        Set<String> allowedFields =
                new HashSet<>(
                        Set.of(fields)
                );

        for (int i = 0;
             i < experience.size();
             i++) {

            JsonNode item =
                    experience.get(i);

            String path =
                    "experience[" + i + "]";

            if (!item.isObject()) {

                throw new RuntimeException(
                        path +
                                " must be a JSON object."
                );
            }

            for (String field : fields) {

                if (!item.has(field)) {

                    throw new RuntimeException(
                            path +
                                    " is missing required field: "
                                    + field
                    );
                }
            }

            validateNullableString(
                    item.path("company"),
                    path + ".company"
            );

            validateNullableString(
                    item.path("role"),
                    path + ".role"
            );

            validateNullableString(
                    item.path("startDate"),
                    path + ".startDate"
            );

            validateNullableString(
                    item.path("endDate"),
                    path + ".endDate"
            );

            validateNullableString(
                    item.path("location"),
                    path + ".location"
            );

            validateStringArray(
                    item.path("responsibilities"),
                    path + ".responsibilities"
            );

            validateUnexpectedFields(
                    item,
                    allowedFields,
                    path
            );
        }
    }

    // ================================================================
    // EDUCATION VALIDATION
    // ================================================================

    private void validateEducation(
            JsonNode education
    ) {

        String[] fields = {

                "degree",
                "institution",
                "startYear",
                "endYear",
                "cgpa"
        };

        Set<String> allowedFields =
                new HashSet<>(
                        Set.of(fields)
                );

        for (int i = 0;
             i < education.size();
             i++) {

            JsonNode item =
                    education.get(i);

            String path =
                    "education[" + i + "]";

            if (!item.isObject()) {

                throw new RuntimeException(
                        path +
                                " must be a JSON object."
                );
            }

            for (String field : fields) {

                if (!item.has(field)) {

                    throw new RuntimeException(
                            path +
                                    " is missing required field: "
                                    + field
                    );
                }
            }

            validateNullableString(
                    item.path("degree"),
                    path + ".degree"
            );

            validateNullableString(
                    item.path("institution"),
                    path + ".institution"
            );

            validateNullableString(
                    item.path("startYear"),
                    path + ".startYear"
            );

            validateNullableString(
                    item.path("endYear"),
                    path + ".endYear"
            );

            validateNullableString(
                    item.path("cgpa"),
                    path + ".cgpa"
            );

            validateUnexpectedFields(
                    item,
                    allowedFields,
                    path
            );
        }
    }

    // ================================================================
    // PROJECT VALIDATION
    // ================================================================

    private void validateProjects(
            JsonNode projects
    ) {

        String[] fields = {

                "name",
                "description",
                "technologies",
                "github",
                "live"
        };

        Set<String> allowedFields =
                new HashSet<>(
                        Set.of(fields)
                );

        for (int i = 0;
             i < projects.size();
             i++) {

            JsonNode item =
                    projects.get(i);

            String path =
                    "projects[" + i + "]";

            if (!item.isObject()) {

                throw new RuntimeException(
                        path +
                                " must be a JSON object."
                );
            }

            for (String field : fields) {

                if (!item.has(field)) {

                    throw new RuntimeException(
                            path +
                                    " is missing required field: "
                                    + field
                    );
                }
            }

            validateNullableString(
                    item.path("name"),
                    path + ".name"
            );

            validateStringArray(
                    item.path("description"),
                    path + ".description"
            );

            validateStringArray(
                    item.path("technologies"),
                    path + ".technologies"
            );

            validateNullableString(
                    item.path("github"),
                    path + ".github"
            );

            validateNullableString(
                    item.path("live"),
                    path + ".live"
            );

            validateUnexpectedFields(
                    item,
                    allowedFields,
                    path
            );
        }
    }

    // ================================================================
    // CERTIFICATION VALIDATION
    // ================================================================

    private void validateCertifications(
            JsonNode certifications
    ) {

        String[] fields = {

                "name",
                "issuer",
                "date"
        };

        Set<String> allowedFields =
                new HashSet<>(
                        Set.of(fields)
                );

        for (int i = 0;
             i < certifications.size();
             i++) {

            JsonNode item =
                    certifications.get(i);

            String path =
                    "certifications[" + i + "]";

            if (!item.isObject()) {

                throw new RuntimeException(
                        path +
                                " must be a JSON object."
                );
            }

            for (String field : fields) {

                if (!item.has(field)) {

                    throw new RuntimeException(
                            path +
                                    " is missing required field: "
                                    + field
                    );
                }
            }

            validateNullableString(
                    item.path("name"),
                    path + ".name"
            );

            validateNullableString(
                    item.path("issuer"),
                    path + ".issuer"
            );

            validateNullableString(
                    item.path("date"),
                    path + ".date"
            );

            validateUnexpectedFields(
                    item,
                    allowedFields,
                    path
            );
        }
    }

    // ================================================================
    // ARRAY VALIDATION
    // ================================================================

    private void validateObjectArray(
            JsonNode field,
            String fieldName
    ) {

        if (!field.isArray()) {

            throw new RuntimeException(
                    "AI response field '" +
                            fieldName +
                            "' must be a JSON array."
            );
        }
    }

    private void validateStringArray(
            JsonNode field,
            String fieldName
    ) {

        if (!field.isArray()) {

            throw new RuntimeException(
                    "AI response field '" +
                            fieldName +
                            "' must be a JSON array."
            );
        }

        for (int i = 0;
             i < field.size();
             i++) {

            JsonNode item =
                    field.get(i);

            if (!item.isTextual()) {

                throw new RuntimeException(
                        "AI response field '" +
                                fieldName +
                                "' must contain only strings. " +
                                "Invalid item at index " +
                                i
                );
            }
        }
    }

    // ================================================================
    // NULLABLE STRING VALIDATION
    // ================================================================

    private void validateNullableString(
            JsonNode field,
            String fieldName
    ) {

        if (field.isNull()) {
            return;
        }

        if (!field.isTextual()) {

            throw new RuntimeException(
                    "AI response field '" +
                            fieldName +
                            "' must be a string or null."
            );
        }
    }

    // ================================================================
    // UNEXPECTED FIELD VALIDATION
    // ================================================================

    private void validateUnexpectedFields(
            JsonNode object,
            Set<String> allowedFields,
            String objectPath
    ) {

        for (Map.Entry<String, JsonNode> entry :
                object.properties()) {

            String actualField =
                    entry.getKey();

            if (!allowedFields.contains(
                    actualField
            )) {

                throw new RuntimeException(
                        "AI response contains unexpected field '" +
                                actualField +
                                "' in " +
                                objectPath
                );
            }
        }
    }
}