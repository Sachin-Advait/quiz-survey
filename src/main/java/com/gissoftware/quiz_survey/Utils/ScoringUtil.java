package com.gissoftware.quiz_survey.Utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class ScoringUtil {

  public static ScoringResult score(
      Map<String, Object> given,
      Map<String, Object> answerKey,
      Map<String, String> questionTypes,
      Map<String, Integer> marks) {

    if (answerKey == null) {
      return new ScoringResult(0);
    }

    int totalScore = 0;

    for (var entry : answerKey.entrySet()) {
      String question = entry.getKey();
      Object expected = entry.getValue();
      Object ans = given.get(question);

      String type = questionTypes.getOrDefault(question, "text");
      int mark = marks.getOrDefault(question, 1);

      if (ans == null) {
        continue;
      }

      boolean isCorrect = false;

      if (type.equalsIgnoreCase("checkbox")) {

        if (ans instanceof Collection<?> givenSet
            && expected instanceof Collection<?> expectedSet) {

          HashSet<String> givenNormalized = normalizeCollection(givenSet);
          HashSet<String> expectedNormalized = normalizeCollection(expectedSet);

          if (givenNormalized.equals(expectedNormalized)) {
            isCorrect = true;
          }
        }

      } else {

        if (answersMatch(ans, expected)) {
          isCorrect = true;
        }
      }

      if (isCorrect) {
        totalScore += mark;
      }
    }

    return new ScoringResult(totalScore);
  }

  /**
   * Compare two answers while ignoring: - leading spaces - trailing spaces - multiple spaces -
   * uppercase/lowercase differences - non-breaking spaces
   */
  private static boolean answersMatch(Object given, Object expected) {

    if (given == null || expected == null) {
      return Objects.equals(given, expected);
    }

    return normalize(given.toString()).equalsIgnoreCase(normalize(expected.toString()));
  }

  /** Normalize an answer before comparison. */
  private static String normalize(String value) {

    if (value == null) {
      return null;
    }

    return value
        .replace('\u00A0', ' ') // non-breaking space
        .trim() // remove beginning/end spaces
        .replaceAll("\\s+", " "); // multiple spaces -> one space
  }

  /** Normalize checkbox values. */
  private static HashSet<String> normalizeCollection(Collection<?> values) {

    HashSet<String> result = new HashSet<>();

    for (Object value : values) {
      if (value != null) {
        result.add(normalize(value.toString()).toLowerCase());
      }
    }

    return result;
  }

  public record ScoringResult(int score) {}
}
