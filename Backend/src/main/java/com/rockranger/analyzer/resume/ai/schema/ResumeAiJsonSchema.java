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
                        Map.of(
                                "type", "array",

                                "items",
                                Map.of(
                                        "type", "string"
                                )
                        ),

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

                                                "title",
                                                nullableString(),

                                                "company",
                                                nullableString(),

                                                "startDate",
                                                nullableString(),

                                                "endDate",
                                                nullableString(),

                                                "details",
                                                Map.of(
                                                        "type", "array",

                                                        "items",
                                                        Map.of(
                                                                "type",
                                                                "string"
                                                        )
                                                )
                                        ),

                                        "required", List.of(
                                                "title",
                                                "company",
                                                "startDate",
                                                "endDate",
                                                "details"
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

                                                "field",
                                                nullableString(),

                                                "startDate",
                                                nullableString(),

                                                "endDate",
                                                nullableString(),

                                                "cgpa",
                                                nullableString()
                                        ),

                                        "required", List.of(
                                                "degree",
                                                "institution",
                                                "field",
                                                "startDate",
                                                "endDate",
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

                                                "title",
                                                nullableString(),

                                                "description",
                                                Map.of(
                                                        "type", "array",

                                                        "items",
                                                        Map.of(
                                                                "type",
                                                                "string"
                                                        )
                                                ),

                                                "technologies",
                                                Map.of(
                                                        "type", "array",

                                                        "items",
                                                        Map.of(
                                                                "type",
                                                                "string"
                                                        )
                                                ),

                                                "github",
                                                nullableString(),

                                                "live",
                                                nullableString()
                                        ),

                                        "required", List.of(
                                                "title",
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
}