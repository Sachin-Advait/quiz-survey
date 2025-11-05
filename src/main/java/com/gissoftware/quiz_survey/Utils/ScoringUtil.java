package com.gissoftware.quiz_survey.Utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class ScoringUtil {

    public static ScoringResult score(
            Map<String, Object> given,
            Map<String, Object> answerKey,
            Map<String, String> questionTypes) {
        if (answerKey == null) return new ScoringResult(0, 0);

        int max = answerKey.size();
        int correct = 0;

        for (var entry : answerKey.entrySet()) {
            String question = entry.getKey();
            Object expected = entry.getValue();
            Object ans = given.get(question);
            String type = questionTypes.getOrDefault(question, "text");

            if (ans == null) continue;

            switch (type) {
                case "checkbox" -> {
                    if (ans instanceof Collection<?> givenSet && expected instanceof Collection<?> expectedSet) {
                        if (new HashSet<>(givenSet).equals(new HashSet<>(expectedSet))) {
                            correct++;
                        }
                    }
                }
                case "boolean" -> {
                    String normalizedAns = ans.toString().equalsIgnoreCase("true") ? "yes" : "no";
                    if (expected.toString().toLowerCase().equalsIgnoreCase(normalizedAns)) {
                        correct++;
                    }
                }
                default -> {
                    if (ans.toString().equalsIgnoreCase(expected.toString())) {
                        correct++;
                    }
                }
            }
        }

        return new ScoringResult(correct, max);
    }

    public record ScoringResult(int score, int max) {
    }
}
