package com.descriptioncreator.backend.generator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class GradeMapperTests {

    private final GradeMapper gradeMapper = new GradeMapper();

    @ParameterizedTest
    @CsvSource({
            "1, Brand New",
            "2, Excellent Condition",
            "3, Very Good Condition",
            "4, Good Condition",
            "5, Used Condition"
    })
    void mapsKnownGrades(String grade, String expectedLabel) {
        assertThat(gradeMapper.map(grade)).isEqualTo(expectedLabel);
    }

    @Test
    void returnsTrimmedUnknownGradeAsIs() {
        assertThat(gradeMapper.map("  Collector Grade  ")).isEqualTo("Collector Grade");
    }

    @Test
    void returnsEmptyStringForNullOrBlankGrade() {
        assertThat(gradeMapper.map(null)).isEmpty();
        assertThat(gradeMapper.map("   ")).isEmpty();
    }
}
