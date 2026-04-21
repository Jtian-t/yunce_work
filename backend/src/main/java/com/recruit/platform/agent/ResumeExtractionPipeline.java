package com.recruit.platform.agent;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ResumeExtractionPipeline {

    PipelineDocument ingest(String rawText) {
        String normalized = normalize(rawText);
        boolean ocrRequired = normalized.isBlank();
        String extractionMode = ocrRequired ? "OCR_REQUIRED" : "TEXT_PDF";
        List<ParseRawBlockResponse> rawBlocks = new ArrayList<>();
        if (!normalized.isBlank()) {
            rawBlocks.add(new ParseRawBlockResponse(
                    "TEXT",
                    "全文片段",
                    normalized.length() > 1200 ? normalized.substring(0, 1200) : normalized
            ));
        }
        return new PipelineDocument(normalized, extractionMode, ocrRequired, rawBlocks);
    }

    List<ParseSkillResponse> toStructuredSkills(List<String> skills) {
        return skills.stream()
                .map(skill -> new ParseSkillResponse(skill, skill, "keyword_match", 0.7))
                .toList();
    }

    List<ParseProjectDetailResponse> toStructuredProjects(List<ParseProjectResponse> projects) {
        return projects.stream()
                .map(project -> new ParseProjectDetailResponse(
                        project.title(),
                        null,
                        null,
                        List.of(),
                        List.of(project.summary()),
                        List.of(),
                        project.summary()
                ))
                .toList();
    }

    private String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        String previous = null;
        for (String rawLine : text.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = rawLine
                    .replace('：', ':')
                    .replace('（', '(')
                    .replace('）', ')')
                    .replace('\t', ' ')
                    .trim();
            if (line.isBlank()) {
                continue;
            }
            if (!line.equals(previous)) {
                lines.add(line);
                previous = line;
            }
        }
        return String.join("\n", lines);
    }
}

record PipelineDocument(
        String normalizedText,
        String extractionMode,
        boolean ocrRequired,
        List<ParseRawBlockResponse> rawBlocks
) {
}
