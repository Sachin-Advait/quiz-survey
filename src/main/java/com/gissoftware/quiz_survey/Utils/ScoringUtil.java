package com.gissoftware.quiz_survey.Utils;

import java.util.Map;

public class ScoringUtil {
    public static ScoringResult score(Map<String, Object> given, Map<String, Object> answerKey) {
        if (answerKey == null) return new ScoringResult(0, 0);
        int max = answerKey.size();
        int correct = 0;
        for (var entry : answerKey.entrySet()) {
            Object ans = given.get(entry.getKey());
            if (ans != null && ans.equals(entry.getValue())) {
                correct++;
            }
        }
        return new ScoringResult(correct, max);
    }

    // ✅ Make the record public
    public record ScoringResult(int score, int max) {
    }
}
