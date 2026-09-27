package com.descriptioncreator.backend.generator;

import org.springframework.stereotype.Component;

@Component
public class GradeMapper {

    public String map(String grade) {
        if (grade == null || grade.isBlank()) {
            return "";
        }

        String normalizedGrade = grade.trim();
        return switch (normalizedGrade) {
            case "1" -> "Brand New";
            case "2" -> "Excellent Condition";
            case "3" -> "Very Good Condition";
            case "4" -> "Good Condition";
            case "5" -> "Used Condition";
            default -> normalizedGrade;
        };
    }
}
