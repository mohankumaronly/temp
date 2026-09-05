package com.rockranger.analyzer.resume.dto.ai;

import java.util.List;

public record ParsedResumeResponse(
        PersonalInfo personalInfo,
        String summary,
        List<String> skills,
        List<Experience> experience,
        List<Education> education,
        List<Project> projects,
        List<Certification> certifications
) {

    public record PersonalInfo(
            String name,
            String email,
            String phone,
            String location,
            String linkedin,
            String github,
            String portfolio
    ) {}

    public record Experience(
            String company,
            String role,
            String startDate,
            String endDate,
            String location,
            List<String> responsibilities
    ) {}

    public record Education(
            String degree,
            String institution,
            String startYear,
            String endYear,
            String cgpa
    ) {}

    public record Project(
            String name,
            List<String> description,
            List<String> technologies,
            String github,
            String live
    ) {}

    public record Certification(
            String name,
            String issuer,
            String date
    ) {}
}