package com.rockranger.analyzer.resume.ai.schema;

import java.util.List;
import java.util.Map;

public final class ResumeAiJsonSchema {

    private ResumeAiJsonSchema() {
        // Prevent object creation
    }

    public static Map<String, Object> build() {

        return Map.of(

                "type", "object",

                "additionalProperties", false,

                "properties", Map.of(

                        // =====================================================
                        // PERSONAL INFORMATION
                        // =====================================================

                        "personalInfo",
                        Map.of(
                                "type", "object",

                                "additionalProperties", false,

                                "properties", Map.of(

                                        "name",
                                        nullableString(),

                                        "email",
                                        nullableString(),

                                        "phone",
                                        nullableString(),

                                        "location",
                                        nullableString(),

                                        "linkedin",
                                        nullableString(),

                                        "github",
                                        nullableString(),

                                        "portfolio",
                                        nullableString()
                                ),

                                "required", List.of(
                                        "name",
                                        "email",
                                        "phone",
                                        "location",
                                        "linkedin",
                                        "github",
                                        "portfolio"
                                )
                        ),

                        // =====================================================
                        // SUMMARY
                        // =====================================================

                        "summary",
                        nullableString(),

                        // =====================================================
                        // SKILLS
                        // =====================================================

                        "skills",
                        arrayOfStrings(),

                        // =====================================================
                        // EXPERIENCE
                        // =====================================================

                        "experience",
                        Map.of(
                                "type", "array",

                                "items",
                                Map.of(
                                        "type", "object",

                                        "additionalProperties", false,

                                        "properties", Map.of(

                                                "company",
                                                nullableString(),

                                                "role",
                                                nullableString(),

                                                "startDate",
                                                nullableString(),

                                                "endDate",
                                                nullableString(),

                                                "location",
                                                nullableString(),

                                                "responsibilities",
                                                arrayOfStrings()
                                        ),

                                        "required", List.of(
                                                "company",
                                                "role",
                                                "startDate",
                                                "endDate",
                                                "location",
                                                "responsibilities"
                                        )
                                )
                        ),

                        // =====================================================
                        // EDUCATION
                        // =====================================================

                        "education",
                        Map.of(
                                "type", "array",

                                "items",
                                Map.of(
                                        "type", "object",

                                        "additionalProperties", false,

                                        "properties", Map.of(

                                                "degree",
                                                nullableString(),

                                                "institution",
                                                nullableString(),

                                                "startYear",
                                                nullableString(),

                                                "endYear",
                                                nullableString(),

                                                "cgpa",
                                                nullableString()
                                        ),

                                        "required", List.of(
                                                "degree",
                                                "institution",
                                                "startYear",
                                                "endYear",
                                                "cgpa"
                                        )
                                )
                        ),

                        // =====================================================
                        // PROJECTS
                        // =====================================================

                        "projects",
                        Map.of(
                                "type", "array",

                                "items",
                                Map.of(
                                        "type", "object",

                                        "additionalProperties", false,

                                        "properties", Map.of(

                                                "name",
                                                nullableString(),

                                                "description",
                                                arrayOfStrings(),

                                                "technologies",
                                                arrayOfStrings(),

                                                "github",
                                                nullableString(),

                                                "live",
                                                nullableString()
                                        ),

                                        "required", List.of(
                                                "name",
                                                "description",
                                                "technologies",
                                                "github",
                                                "live"
                                        )
                                )
                        ),

                        // =====================================================
                        // CERTIFICATIONS
                        // =====================================================

                        "certifications",
                        Map.of(
                                "type", "array",

                                "items",
                                Map.of(
                                        "type", "object",

                                        "additionalProperties", false,

                                        "properties", Map.of(

                                                "name",
                                                nullableString(),

                                                "issuer",
                                                nullableString(),

                                                "date",
                                                nullableString()
                                        ),

                                        "required", List.of(
                                                "name",
                                                "issuer",
                                                "date"
                                        )
                                )
                        )
                ),

                // =============================================================
                // REQUIRED TOP-LEVEL FIELDS
                // =============================================================

                "required", List.of(
                        "personalInfo",
                        "summary",
                        "skills",
                        "experience",
                        "education",
                        "projects",
                        "certifications"
                )
        );
    }

    /**
     * Creates a schema property that accepts either:
     *
     * String
     * OR
     * null
     */
    private static Map<String, Object> nullableString() {

        return Map.of(
                "type",
                List.of("string", "null")
        );
    }

    /**
     * Creates a schema property representing:
     *
     * array of strings
     */
    private static Map<String, Object> arrayOfStrings() {

        return Map.of(
                "type", "array",

                "items",
                Map.of(
                        "type", "string"
                )
        );
    }
}