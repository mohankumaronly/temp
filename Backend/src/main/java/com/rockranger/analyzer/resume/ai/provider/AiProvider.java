package com.rockranger.analyzer.resume.ai.provider;

public interface AiProvider {

    String generate(
            String systemPrompt,
            String userPrompt
    );
}