package com.slmapp.backend.magi;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ComplexityClassifier {

    private static final List<String> COMPLEX_MARKERS = List.of(
            "should i", "which is better", "pros and cons", "compare", " vs ",
            "what do you think", "is it worth", "recommend", "advice"
    );

    public boolean isComplex(String prompt) {
        if (prompt == null) {
            return false;
        }
        String lower = prompt.toLowerCase();

        if (lower.split("\\s+").length > 40) {
            return true;
        }

        return COMPLEX_MARKERS.stream().anyMatch(lower::contains);
    }
}
