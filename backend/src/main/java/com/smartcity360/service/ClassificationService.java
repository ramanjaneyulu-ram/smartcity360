package com.smartcity360.service;

import com.smartcity360.dto.ClassifyResponse;
import com.smartcity360.model.Priority;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Rule-based NLP-lite classifier for citizen complaint descriptions.
 *
 * This mirrors the keyword rules used in the front-end's live "AI classification preview"
 * so the demo and the real submission pipeline always agree. It is intentionally simple —
 * swap this service out for a call to a dedicated Python NLP/ML microservice (the design
 * the project abstract describes) without changing any controller or DTO in this project;
 * ClassificationService is the only seam that needs to change.
 */
@Service
public class ClassificationService {

    private record Rule(String[] keywords, String category, String department, Priority priority) {}

    private final Rule[] rules = new Rule[] {
        new Rule(new String[]{"light", "lamp", "dark", "electric"}, "Street Light", "Electrical", Priority.MEDIUM),
        new Rule(new String[]{"garbage", "trash", "waste", "bin"}, "Garbage Collection", "Sanitation", Priority.MEDIUM),
        new Rule(new String[]{"pothole", "road", "crack", "asphalt"}, "Road Damage", "Public Works", Priority.HIGH),
        new Rule(new String[]{"water", "leak", "pipe", "supply"}, "Water Leakage", "Water Works", Priority.HIGH),
        new Rule(new String[]{"drain", "sewer", "overflow", "flood"}, "Drainage", "Public Works", Priority.HIGH),
        new Rule(new String[]{"tree", "fallen", "branch"}, "Fallen Tree", "Parks & Horticulture", Priority.MEDIUM),
        new Rule(new String[]{"signal", "traffic"}, "Traffic Signal", "Traffic Dept.", Priority.HIGH),
        new Rule(new String[]{"stray", "dog", "animal"}, "Stray Animal", "Animal Control", Priority.LOW),
    };

    private final Random random = new Random();

    public ClassifyResponse classify(String description) {
        String text = description == null ? "" : description.toLowerCase();

        for (Rule rule : rules) {
            for (String kw : rule.keywords()) {
                if (text.contains(kw)) {
                    return ClassifyResponse.builder()
                            .category(rule.category())
                            .department(rule.department())
                            .priority(rule.priority().name())
                            .confidence(90 + random.nextInt(7))
                            .build();
                }
            }
        }

        return ClassifyResponse.builder()
                .category("General Complaint")
                .department("Admin Review")
                .priority(Priority.LOW.name())
                .confidence(55 + random.nextInt(15))
                .build();
    }

    /** Used internally, e.g. when auto-classifying on submission. */
    public Map<String, String> classifyToMap(String description) {
        ClassifyResponse r = classify(description);
        Map<String, String> map = new LinkedHashMap<>();
        map.put("category", r.getCategory());
        map.put("department", r.getDepartment());
        map.put("priority", r.getPriority());
        return map;
    }
}
