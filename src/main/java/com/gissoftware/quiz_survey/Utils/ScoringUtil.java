package com.gissoftware.quiz_survey.Utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class ScoringUtil {

    public static ScoringResult score(
            Map<String, Object> given,
            Map<String, Object> answerKey,
            Map<String, String> questionTypes,
            Map<String, Integer> marks) {
        if (answerKey == null) return new ScoringResult(0);

        int totalScore = 0;

        for (var entry : answerKey.entrySet()) {
            String question = entry.getKey();
            Object expected = entry.getValue();
            Object ans = given.get(question);
            String type = questionTypes.getOrDefault(question, "text");
            int mark = marks.get(question);


            if (ans == null) continue;

            boolean isCorrect = false;
            if (type.equals("checkbox")) {
                if (ans instanceof Collection<?> givenSet && expected instanceof Collection<?> expectedSet) {
                    if (new HashSet<>(givenSet).equals(new HashSet<>(expectedSet))) {
                        isCorrect = true;
                    }
                }
            } else {
                if (ans.toString().equalsIgnoreCase(expected.toString())) {
                    isCorrect = true;
                }
            }

            if (isCorrect) {
                totalScore += mark;
            }
        }

        return new ScoringResult(totalScore);
    }

    public record ScoringResult(int score) {
    }
}
