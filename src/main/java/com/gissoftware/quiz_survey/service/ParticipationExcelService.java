package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.OverallParticipationDTO;
import com.gissoftware.quiz_survey.dto.ParticipationStatusDTO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipationExcelService {

  private final ParticipationService participationService;
  private final OverallParticipationService overallParticipationService;

  public ByteArrayInputStream generateByQuizSurvey(String quizSurveyId) throws Exception {
    List<ParticipationStatusDTO> data = participationService.getParticipationStatus(quizSurveyId);
    return buildExcel(data, false);
  }

  public ByteArrayInputStream generateOverall() throws Exception {
    List<OverallParticipationDTO> data = overallParticipationService.getOverallParticipation();
    return buildExcel(data, true);
  }

  private ByteArrayInputStream buildExcel(List<?> list, boolean overall) throws Exception {

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      CellStyle percentStyle = workbook.createCellStyle();
      percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

      boolean isSurvey =
          !overall
              && list.stream()
                  .filter(ParticipationStatusDTO.class::isInstance)
                  .map(ParticipationStatusDTO.class::cast)
                  .allMatch(d -> d.getScore() == null && d.getMaxScore() == null);

      boolean isQuiz = !overall && !isSurvey;

      String sheetName =
          overall
              ? "Overall-Quiz-Survey-Report"
              : isSurvey ? "Per Survey Participation" : "Per Quiz Participation";

      Sheet sheet = workbook.createSheet(sheetName);

      // ---------------------------------------------------------------
      // OVERALL mode
      // Each row uses its OWN questions starting from Q1/1.
      // Column order:
      //   Title | Type | StaffId | Username | Region | Outlet
      //   | Q1/1 | Q2/2 | ... (up to maxQuestions across all rows)
      //   | A1 | A2 | ...
      //   | Completion | Quiz Open Time | Agent Open Time | Agent Submission Time
      //   | Score | MaxScore | Percentage | Result
      //   | Correct Answer | Correct answer 2 | ...  (blank for survey rows)
      // ---------------------------------------------------------------
      if (overall) {

        @SuppressWarnings("unchecked")
        List<OverallParticipationDTO> data = (List<OverallParticipationDTO>) list;

        // Find the maximum number of questions any single row has
        int maxQuestions =
            data.stream()
                .mapToInt(d -> d.getQuestionAnswers() != null ? d.getQuestionAnswers().size() : 0)
                .max()
                .orElse(0);

        // Build header row
        Row header = sheet.createRow(0);
        int hCol = 0;

        header.createCell(hCol++).setCellValue("Title");
        header.createCell(hCol++).setCellValue("Type");
        header.createCell(hCol++).setCellValue("StaffId");
        header.createCell(hCol++).setCellValue("Username");
        header.createCell(hCol++).setCellValue("Region");
        header.createCell(hCol++).setCellValue("Outlet");

        // Meta columns
        header.createCell(hCol++).setCellValue("Completion");
        header.createCell(hCol++).setCellValue("Quiz Open Date");
        header.createCell(hCol++).setCellValue("Quiz Open Time");
        header.createCell(hCol++).setCellValue("Agent Open Date");
        header.createCell(hCol++).setCellValue("Agent Open Time");
        header.createCell(hCol++).setCellValue("Agent Submission Date");
        header.createCell(hCol++).setCellValue("Agent Submission Time");
        header.createCell(hCol++).setCellValue("Time Taken");
        header.createCell(hCol++).setCellValue("Score");
        header.createCell(hCol++).setCellValue("MaxScore");
        header.createCell(hCol++).setCellValue("Percentage");
        header.createCell(hCol++).setCellValue("Result");

        // Q columns headers - just Q1, Q2, Q3 without marks
        for (int i = 1; i <= maxQuestions; i++) {
          header.createCell(hCol++).setCellValue("Q" + i);
        }

        // Single Question Reference column
        header.createCell(hCol++).setCellValue("Question Reference");

        int totalCols = hCol;

        // Write data rows
        int rowIdx = 1;
        for (OverallParticipationDTO d : data) {
          Row row = sheet.createRow(rowIdx++);
          int col = 0;

          row.createCell(col++).setCellValue(d.getTitle() != null ? d.getTitle() : "");
          row.createCell(col++).setCellValue(d.getType() != null ? d.getType() : "");
          row.createCell(col++).setCellValue(d.getStaffId() != null ? d.getStaffId() : "");
          row.createCell(col++).setCellValue(d.getUsername() != null ? d.getUsername() : "");
          row.createCell(col++).setCellValue(d.getRegion() != null ? d.getRegion() : "");
          row.createCell(col++).setCellValue(d.getOutlet() != null ? d.getOutlet() : "");

          boolean isSurveyRow = "survey".equalsIgnoreCase(d.getType());

          // Meta columns
          row.createCell(col++).setCellValue(d.getCompletion() != null ? d.getCompletion() : false);

          row.createCell(col++).setCellValue(formatDate(d.getQuizOpenTime()));
          row.createCell(col++).setCellValue(formatTime(d.getQuizOpenTime()));

          row.createCell(col++).setCellValue(formatDate(d.getAgentOpenTime()));
          row.createCell(col++).setCellValue(formatTime(d.getAgentOpenTime()));

          row.createCell(col++).setCellValue(formatDate(d.getAgentSubmissionTime()));
          row.createCell(col++).setCellValue(formatTime(d.getAgentSubmissionTime()));

          row.createCell(col++)
              .setCellValue(calculateDuration(d.getAgentOpenTime(), d.getAgentSubmissionTime()));

          if (d.getScore() != null) row.createCell(col).setCellValue(d.getScore());
          col++;

          if (d.getMaxScore() != null) row.createCell(col).setCellValue(d.getMaxScore());
          col++;

          if (d.getPercentage() != null) {
            row.createCell(col).setCellValue(d.getPercentage() / 100.0);
            row.getCell(col).setCellStyle(percentStyle);
          }
          col++;

          row.createCell(col++).setCellValue(d.getResult() != null ? d.getResult() : "");

          // Get this row's questions in insertion order
          List<String> thisRowKeys =
              d.getQuestionAnswers() != null
                  ? new ArrayList<>(d.getQuestionAnswers().keySet())
                  : new ArrayList<>();
          int thisRowQCount = thisRowKeys.size();

          // Q columns — show "obtainedMarks/totalMarks" format
          for (int i = 0; i < maxQuestions; i++) {
            if (i < thisRowQCount && !isSurveyRow) {
              String questionKey = thisRowKeys.get(i);
              String userAnswer =
                  d.getQuestionAnswers() != null ? d.getQuestionAnswers().get(questionKey) : "";
              String correctAnswer =
                  d.getCorrectAnswers() != null ? d.getCorrectAnswers().get(questionKey) : "";
              Integer totalMarks =
                  d.getQuestionMarks() != null
                      ? d.getQuestionMarks().getOrDefault(questionKey, 0)
                      : 0;

              int obtainedMarks = 0;
              // If answer matches correct answer, give full marks for that question
              if (userAnswer != null
                  && correctAnswer != null
                  && userAnswer.trim().equalsIgnoreCase(correctAnswer.trim())) {
                obtainedMarks = totalMarks;
              }

              row.createCell(col++).setCellValue(obtainedMarks + "/" + totalMarks);
            } else if (i < thisRowQCount && isSurveyRow) {
              // For survey rows, leave blank
              row.createCell(col++).setCellValue("");
            } else {
              row.createCell(col++).setCellValue("");
            }
          }

          // Question Reference column - comma separated question references for this row
          StringBuilder questionRef = new StringBuilder();
          for (int i = 0; i < thisRowQCount; i++) {
            if (i > 0) {
              questionRef.append(",");
            }
            questionRef.append("Q").append(i + 1).append(" - ").append(thisRowKeys.get(i));
          }
          row.createCell(col++).setCellValue(questionRef.toString());
        }

        for (int i = 0; i < totalCols; i++) sheet.autoSizeColumn(i);
      }

      // ---------------------------------------------------------------
      // QUIZ mode
      //   Title | StaffId | Username | Region | Outlet
      //   | Q1/1 | Q2/2 | ...
      //   | A1 | A2 | ...
      //   | Completion | Quiz Open Time | Agent Open Time | Agent Submission Time
      //   | Score | MaxScore | Percentage | Result
      //   | Correct Answer | Correct answer 2 | ...
      // ---------------------------------------------------------------
      //      else if (isQuiz) {
      //
      //        @SuppressWarnings("unchecked")
      //        List<ParticipationStatusDTO> data = (List<ParticipationStatusDTO>) list;
      //
      //        Set<String> questionSet = new LinkedHashSet<>();
      //        for (ParticipationStatusDTO d : data) {
      //          if (d.getQuestion() != null) questionSet.add(d.getQuestion());
      //        }
      //
      //        List<String> questionList = new ArrayList<>(questionSet);
      //        int totalQuestions = questionList.size();
      //
      //        Map<String, List<ParticipationStatusDTO>> byUser =
      //            data.stream()
      //                .collect(
      //                    Collectors.groupingBy(
      //                        d -> d.getStaffId() != null ? d.getStaffId() : "",
      //                        LinkedHashMap::new,
      //                        Collectors.toList()));
      //
      //        Row header = sheet.createRow(0);
      //        int hCol = 0;
      //        header.createCell(hCol++).setCellValue("Title");
      //        header.createCell(hCol++).setCellValue("StaffId");
      //        header.createCell(hCol++).setCellValue("Username");
      //        header.createCell(hCol++).setCellValue("Region");
      //        header.createCell(hCol++).setCellValue("Outlet");
      //
      //        header.createCell(hCol++).setCellValue("Completion");
      //        header.createCell(hCol++).setCellValue("Quiz Open Date");
      //        header.createCell(hCol++).setCellValue("Quiz Open Time");
      //        header.createCell(hCol++).setCellValue("Agent Open Date");
      //        header.createCell(hCol++).setCellValue("Agent Open Time");
      //        header.createCell(hCol++).setCellValue("Agent Submission Date");
      //        header.createCell(hCol++).setCellValue("Agent Submission Time");
      //        header.createCell(hCol++).setCellValue("Time Taken");
      //        header.createCell(hCol++).setCellValue("Score");
      //        header.createCell(hCol++).setCellValue("MaxScore");
      //        header.createCell(hCol++).setCellValue("Percentage");
      //        header.createCell(hCol++).setCellValue("Result");
      //        for (int i = 1; i <= totalQuestions; i++) {
      //          header.createCell(hCol++).setCellValue("Q" + i);
      //        }
      //        for (int i = 1; i <= totalQuestions; i++) {
      //          header.createCell(hCol++).setCellValue("A" + i);
      //        }
      //
      //        for (int i = 1; i <= totalQuestions; i++) {
      //          header.createCell(hCol++).setCellValue(i == 1 ? "Correct Answer" : "Correct answer
      // " + i);
      //        }
      //
      //        int totalCols = hCol;
      //
      //        int rowIdx = 1;
      //        for (Map.Entry<String, List<ParticipationStatusDTO>> entry : byUser.entrySet()) {
      //          List<ParticipationStatusDTO> userRows = entry.getValue();
      //          ParticipationStatusDTO first = userRows.get(0);
      //
      //          Map<String, ParticipationStatusDTO> byQuestion =
      //              userRows.stream()
      //                  .filter(d -> d.getQuestion() != null)
      //                  .collect(
      //                      Collectors.toMap(
      //                          ParticipationStatusDTO::getQuestion,
      //                          d -> d,
      //                          (a, b) -> a,
      //                          LinkedHashMap::new));
      //
      //          Row row = sheet.createRow(rowIdx++);
      //          int col = 0;
      //
      //          row.createCell(col++).setCellValue(first.getTitle() != null ? first.getTitle() :
      // "");
      //          row.createCell(col++).setCellValue(first.getStaffId() != null ? first.getStaffId()
      // : "");
      //          row.createCell(col++)
      //              .setCellValue(first.getUsername() != null ? first.getUsername() : "");
      //          row.createCell(col++).setCellValue(first.getRegion() != null ? first.getRegion() :
      // "");
      //          row.createCell(col++).setCellValue(first.getOutlet() != null ? first.getOutlet() :
      // "");
      //          row.createCell(col++)
      //              .setCellValue(first.getCompletion() != null ? first.getCompletion() : false);
      //
      //          row.createCell(col++).setCellValue(formatDate(first.getQuizOpenTime()));
      //          row.createCell(col++).setCellValue(formatTime(first.getQuizOpenTime()));
      //
      //          row.createCell(col++).setCellValue(formatDate(first.getAgentOpenTime()));
      //          row.createCell(col++).setCellValue(formatTime(first.getAgentOpenTime()));
      //
      //          row.createCell(col++).setCellValue(formatDate(first.getAgentSubmissionTime()));
      //          row.createCell(col++).setCellValue(formatTime(first.getAgentSubmissionTime()));
      //
      //          row.createCell(col++)
      //              .setCellValue(
      //                  calculateDuration(first.getAgentOpenTime(),
      // first.getAgentSubmissionTime()));
      //
      //          if (first.getScore() != null) row.createCell(col).setCellValue(first.getScore());
      //          col++;
      //          if (first.getMaxScore() != null)
      // row.createCell(col).setCellValue(first.getMaxScore());
      //          col++;
      //          if (first.getPercentage() != null) {
      //            row.createCell(col).setCellValue(first.getPercentage() / 100.0);
      //            row.getCell(col).setCellStyle(percentStyle);
      //          }
      //          col++;
      //          row.createCell(col++).setCellValue(first.getResult() != null ? first.getResult() :
      // "");
      //          // Q columns — question title text as cell value
      //          for (String q : questionList) {
      //            row.createCell(col++).setCellValue(q);
      //          }
      //
      //          // A columns — agent answers
      //          for (String q : questionList) {
      //            ParticipationStatusDTO qRow = byQuestion.get(q);
      //            String ans = qRow != null && qRow.getAgentAnswer() != null ?
      // qRow.getAgentAnswer() : "";
      //            row.createCell(col++).setCellValue(ans);
      //          }
      //
      //          // Correct Answer columns — at the end
      //          for (String q : questionList) {
      //            ParticipationStatusDTO qRow = byQuestion.get(q);
      //            String ca =
      //                qRow != null && qRow.getCorrectAnswer() != null ? qRow.getCorrectAnswer() :
      // "";
      //            row.createCell(col++).setCellValue(ca);
      //          }
      //        }
      //
      //        for (int i = 0; i < totalCols; i++) sheet.autoSizeColumn(i);
      //      }
      else if (isQuiz) {

        @SuppressWarnings("unchecked")
        List<ParticipationStatusDTO> data = (List<ParticipationStatusDTO>) list;

        Set<String> questionSet = new LinkedHashSet<>();
        Map<String, Integer> questionMarksMap = new LinkedHashMap<>(); // Store marks per question
        for (ParticipationStatusDTO d : data) {
          if (d.getQuestion() != null) {
            questionSet.add(d.getQuestion());
            // Store marks for each question if not already stored
            if (!questionMarksMap.containsKey(d.getQuestion()) && d.getMarks() != null) {
              questionMarksMap.put(d.getQuestion(), d.getMarks());
            }
          }
        }

        List<String> questionList = new ArrayList<>(questionSet);
        int totalQuestions = questionList.size();

        Map<String, List<ParticipationStatusDTO>> byUser =
            data.stream()
                .collect(
                    Collectors.groupingBy(
                        d -> d.getStaffId() != null ? d.getStaffId() : "",
                        LinkedHashMap::new,
                        Collectors.toList()));

        Row header = sheet.createRow(0);
        int hCol = 0;

        header.createCell(hCol++).setCellValue("Title");
        header.createCell(hCol++).setCellValue("StaffId");
        header.createCell(hCol++).setCellValue("Username");
        header.createCell(hCol++).setCellValue("Region");
        header.createCell(hCol++).setCellValue("Outlet");

        header.createCell(hCol++).setCellValue("Completion");
        header.createCell(hCol++).setCellValue("Quiz Open Date");
        header.createCell(hCol++).setCellValue("Quiz Open Time");
        header.createCell(hCol++).setCellValue("Agent Open Date");
        header.createCell(hCol++).setCellValue("Agent Open Time");
        header.createCell(hCol++).setCellValue("Agent Submission Date");
        header.createCell(hCol++).setCellValue("Agent Submission Time");
        header.createCell(hCol++).setCellValue("Time Taken");
        header.createCell(hCol++).setCellValue("Score");
        header.createCell(hCol++).setCellValue("MaxScore");
        header.createCell(hCol++).setCellValue("Percentage");
        header.createCell(hCol++).setCellValue("Result");

        // Q columns with marks
        for (int i = 0; i < questionList.size(); i++) {
          String q = questionList.get(i);
          Integer marks = questionMarksMap.getOrDefault(q, 0);
          header.createCell(hCol++).setCellValue("Q" + (i + 1) + "/" + marks);
        }

        // Single Question Reference column
        header.createCell(hCol++).setCellValue("Question Reference");

        int totalCols = hCol;

        int rowIdx = 1;
        for (Map.Entry<String, List<ParticipationStatusDTO>> entry : byUser.entrySet()) {

          List<ParticipationStatusDTO> userRows = entry.getValue();
          ParticipationStatusDTO first = userRows.get(0);

          Map<String, ParticipationStatusDTO> byQuestion =
              userRows.stream()
                  .filter(d -> d.getQuestion() != null)
                  .collect(
                      Collectors.toMap(
                          ParticipationStatusDTO::getQuestion,
                          d -> d,
                          (a, b) -> a,
                          LinkedHashMap::new));

          Row row = sheet.createRow(rowIdx++);
          int col = 0;

          row.createCell(col++).setCellValue(first.getTitle() != null ? first.getTitle() : "");
          row.createCell(col++).setCellValue(first.getStaffId() != null ? first.getStaffId() : "");
          row.createCell(col++)
              .setCellValue(first.getUsername() != null ? first.getUsername() : "");
          row.createCell(col++).setCellValue(first.getRegion() != null ? first.getRegion() : "");
          row.createCell(col++).setCellValue(first.getOutlet() != null ? first.getOutlet() : "");

          row.createCell(col++)
              .setCellValue(first.getCompletion() != null ? first.getCompletion() : false);

          row.createCell(col++).setCellValue(formatDate(first.getQuizOpenTime()));
          row.createCell(col++).setCellValue(formatTime(first.getQuizOpenTime()));

          row.createCell(col++).setCellValue(formatDate(first.getAgentOpenTime()));
          row.createCell(col++).setCellValue(formatTime(first.getAgentOpenTime()));

          row.createCell(col++).setCellValue(formatDate(first.getAgentSubmissionTime()));
          row.createCell(col++).setCellValue(formatTime(first.getAgentSubmissionTime()));

          row.createCell(col++)
              .setCellValue(
                  calculateDuration(first.getAgentOpenTime(), first.getAgentSubmissionTime()));

          if (first.getScore() != null) {
            row.createCell(col).setCellValue(first.getScore());
          }
          col++;

          if (first.getMaxScore() != null) {
            row.createCell(col).setCellValue(first.getMaxScore());
          }
          col++;

          if (first.getPercentage() != null) {
            row.createCell(col).setCellValue(first.getPercentage() / 100.0);
            row.getCell(col).setCellStyle(percentStyle);
          }
          col++;

          row.createCell(col++).setCellValue(first.getResult() != null ? first.getResult() : "");

          // Q1/marks, Q2/marks, ...
          for (String q : questionList) {

            ParticipationStatusDTO qRow = byQuestion.get(q);

            Integer marks = 0;

            if (qRow != null
                && qRow.getAgentAnswer() != null
                && qRow.getCorrectAnswer() != null
                && qRow.getAgentAnswer().trim().equalsIgnoreCase(qRow.getCorrectAnswer().trim())) {

              marks = qRow.getMarks() != null ? qRow.getMarks() : 1;
            }

            row.createCell(col++).setCellValue(marks);
          }

          // Question Reference column - comma separated question references
          StringBuilder questionRef = new StringBuilder();
          for (int i = 0; i < questionList.size(); i++) {
            if (i > 0) {
              questionRef.append(",");
            }
            questionRef.append("Q").append(i + 1).append(" - ").append(questionList.get(i));
          }
          row.createCell(col++).setCellValue(questionRef.toString());
        }

        for (int i = 0; i < totalCols; i++) {
          sheet.autoSizeColumn(i);
        }
      }
      // ---------------------------------------------------------------
      // SURVEY mode
      //   Title | StaffId | Username | Region | Outlet
      //   | Q1/1 | Q2/2 | ...
      //   | A1 | A2 | ...
      //   | Completion | Quiz Open Time | Agent Open Time | Agent Submission Time
      //   | Participated | Result
      //   (NO correct answer columns)
      // ---------------------------------------------------------------
      else {

        @SuppressWarnings("unchecked")
        List<ParticipationStatusDTO> data = (List<ParticipationStatusDTO>) list;

        Set<String> questionSet = new LinkedHashSet<>();
        for (ParticipationStatusDTO d : data) {
          if (d.getQuestion() != null) questionSet.add(d.getQuestion());
        }

        List<String> questionList = new ArrayList<>(questionSet);
        int totalQuestions = questionList.size();

        Map<String, List<ParticipationStatusDTO>> byUser =
            data.stream()
                .collect(
                    Collectors.groupingBy(
                        d -> d.getStaffId() != null ? d.getStaffId() : "",
                        LinkedHashMap::new,
                        Collectors.toList()));

        Row header = sheet.createRow(0);
        int hCol = 0;
        header.createCell(hCol++).setCellValue("Title");
        header.createCell(hCol++).setCellValue("StaffId");
        header.createCell(hCol++).setCellValue("Username");
        header.createCell(hCol++).setCellValue("Region");
        header.createCell(hCol++).setCellValue("Outlet");

        header.createCell(hCol++).setCellValue("Completion");
        header.createCell(hCol++).setCellValue("Survey Open Date");
        header.createCell(hCol++).setCellValue("Survey Open Time");
        header.createCell(hCol++).setCellValue("Agent Open Date");
        header.createCell(hCol++).setCellValue("Agent Open Time");
        header.createCell(hCol++).setCellValue("Agent Submission Date");
        header.createCell(hCol++).setCellValue("Agent Submission Time");
        header.createCell(hCol++).setCellValue("Time Taken");
        header.createCell(hCol++).setCellValue("Participated");
        header.createCell(hCol++).setCellValue("Result");

        //        for (int i = 1; i <= totalQuestions; i++) {
        //          header.createCell(hCol++).setCellValue("Q" + i);
        //        }
        //        for (int i = 1; i <= totalQuestions; i++) {
        //          header.createCell(hCol++).setCellValue("A" + i);
        //        }

        int totalCols = hCol;

        int rowIdx = 1;
        for (Map.Entry<String, List<ParticipationStatusDTO>> entry : byUser.entrySet()) {
          List<ParticipationStatusDTO> userRows = entry.getValue();
          ParticipationStatusDTO first = userRows.get(0);

          Map<String, ParticipationStatusDTO> byQuestion =
              userRows.stream()
                  .filter(d -> d.getQuestion() != null)
                  .collect(
                      Collectors.toMap(
                          ParticipationStatusDTO::getQuestion,
                          d -> d,
                          (a, b) -> a,
                          LinkedHashMap::new));

          Row row = sheet.createRow(rowIdx++);
          int col = 0;

          row.createCell(col++).setCellValue(first.getTitle() != null ? first.getTitle() : "");
          row.createCell(col++).setCellValue(first.getStaffId() != null ? first.getStaffId() : "");
          row.createCell(col++)
              .setCellValue(first.getUsername() != null ? first.getUsername() : "");
          row.createCell(col++).setCellValue(first.getRegion() != null ? first.getRegion() : "");
          row.createCell(col++).setCellValue(first.getOutlet() != null ? first.getOutlet() : "");

          row.createCell(col++)
              .setCellValue(first.getCompletion() != null ? first.getCompletion() : false);

          row.createCell(col++).setCellValue(formatDate(first.getQuizOpenTime()));
          row.createCell(col++).setCellValue(formatTime(first.getQuizOpenTime()));

          row.createCell(col++).setCellValue(formatDate(first.getAgentOpenTime()));
          row.createCell(col++).setCellValue(formatTime(first.getAgentOpenTime()));

          row.createCell(col++).setCellValue(formatDate(first.getAgentSubmissionTime()));
          row.createCell(col++).setCellValue(formatTime(first.getAgentSubmissionTime()));

          row.createCell(col++)
              .setCellValue(
                  calculateDuration(first.getAgentOpenTime(), first.getAgentSubmissionTime()));

          row.createCell(col++).setCellValue(first.isParticipated());

          row.createCell(col++).setCellValue(first.getResult() != null ? first.getResult() : "");

          //          // Q columns — question title text as cell value
          //          for (String q : questionList) {
          //            row.createCell(col++).setCellValue(q);
          //          }
          //
          //          // A columns — agent answers only
          //          for (String q : questionList) {
          //            ParticipationStatusDTO qRow = byQuestion.get(q);
          //            String ans = qRow != null && qRow.getAgentAnswer() != null ?
          // qRow.getAgentAnswer() : "";
          //            row.createCell(col++).setCellValue(ans);
          //          }
        }

        for (int i = 0; i < totalCols; i++) sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return new ByteArrayInputStream(out.toByteArray());
    }
  }

  private String formatInstant(Instant instant) {
    if (instant == null) return "";
    return DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")
        .withZone(ZoneId.of("Asia/Riyadh"))
        .format(instant);
  }

  private String formatDate(Instant instant) {
    if (instant == null) return "";
    return DateTimeFormatter.ofPattern("dd MMM yyyy")
        .withZone(ZoneId.of("Asia/Riyadh"))
        .format(instant);
  }

  private String formatTime(Instant instant) {
    if (instant == null) return "";
    return DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.of("Asia/Riyadh"))
        .format(instant);
  }

  private String calculateDuration(Instant start, Instant end) {
    if (start == null || end == null) return "";

    long seconds = java.time.Duration.between(start, end).getSeconds();

    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;

    return String.format("%02d:%02d:%02d", hours, minutes, secs);
  }
}
