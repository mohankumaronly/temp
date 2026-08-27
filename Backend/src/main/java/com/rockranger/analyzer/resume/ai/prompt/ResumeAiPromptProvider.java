package com.rockranger.analyzer.resume.ai.prompt;

import org.springframework.stereotype.Component;

@Component
public class ResumeAiPromptProvider {

    public String getSystemPrompt() {

        return ResumeAiPrompt.SYSTEM_PROMPT;
    }

    public String buildUserPrompt(String extractedText) {

        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException(
                    "Resume extracted text cannot be empty."
            );
        }

        return """
                Extract the structured information from the following resume.

                RESUME TEXT:

                %s
                """.formatted(extractedText);
    }
}