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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipationExcelService {

  private static final Logger logger = LoggerFactory.getLogger(ParticipationExcelService.class);

  private final ParticipationService participationService;
  private final OverallParticipationService overallParticipationService;

  public ByteArrayInputStream generateByQuizSurvey(String quizSurveyId) throws Exception {
    List<ParticipationStatusDTO> data = participationService.getParticipationStatus(quizSurveyId);
    return buildExcel(data, false, null);
  }

  public ByteArrayInputStream generateOverall() throws Exception {
    List<OverallParticipationDTO> data = overallParticipationService.getOverallParticipation();
    return buildExcel(data, true, "overall");
  }

  public ByteArrayInputStream generateOverallQuiz() throws Exception {
    List<OverallParticipationDTO> data = overallParticipationService.getOverallQuizParticipation();
    return buildExcel(data, true, "quiz");
  }

  public ByteArrayInputStream generateOverallSurvey() throws Exception {
    List<OverallParticipationDTO> data =
        overallParticipationService.getOverallSurveyParticipation();
    return buildExcel(data, true, "survey");
  }

  private ByteArrayInputStream buildExcel(List<?> list, boolean overall, String overallType)
      throws Exception {

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      try {
        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

        boolean isSurvey =
            !overall
                && list.stream()
                    .filter(ParticipationStatusDTO.class::isInstance)
                    .map(ParticipationStatusDTO.class::cast)
                    .allMatch(d -> d.getScore() == null && d.getMaxScore() == null);

        boolean isQuiz = !overall && !isSurvey;

        String sheetName;
        if (overall) {
          if ("quiz".equals(overallType)) {
            sheetName = "Overall-Quiz-Report";
          } else if ("survey".equals(overallType)) {
            sheetName = "Overall-Survey-Report";
          } else {
            sheetName = "Overall-Quiz-Survey-Report";
          }
        } else {
          sheetName = isSurvey ? "Per Survey Participation" : "Per Quiz Participation";
        }

        Sheet sheet = workbook.createSheet(sheetName);

        // ---------------------------------------------------------------
        // OVERALL mode
        // ---------------------------------------------------------------
        if (overall) {

          @SuppressWarnings("unchecked")
          List<OverallParticipationDTO> data = (List<OverallParticipationDTO>) list;

          // Check if data is empty
          if (data == null || data.isEmpty()) {
            workbook.removeSheetAt(workbook.getSheetIndex(sheetName));
            Sheet infoSheet = workbook.createSheet("No Data");
            Row headerRow = infoSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Message");
            Row dataRow = infoSheet.createRow(1);
            String type =
                "quiz".equals(overallType)
                    ? "quizzes"
                    : "survey".equals(overallType) ? "surveys" : "quizzes & surveys";
            dataRow.createCell(0).setCellValue("No " + type + " found to export");
            infoSheet.autoSizeColumn(0);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
          }

          // Group data by quiz survey
          Map<String, List<OverallParticipationDTO>> byQuizSurvey =
              data.stream()
                  .collect(
                      Collectors.groupingBy(
                          d -> d.getQuizSurveyId() != null ? d.getQuizSurveyId() : "",
                          LinkedHashMap::new,
                          Collectors.toList()));

          // Remove the main sheet
          workbook.removeSheetAt(workbook.getSheetIndex(sheetName));

          // Create a separate sheet for each quiz/survey
          for (Map.Entry<String, List<OverallParticipationDTO>> entry : byQuizSurvey.entrySet()) {
            List<OverallParticipationDTO> quizData = entry.getValue();

            if (quizData.isEmpty()) continue;

            OverallParticipationDTO firstRow = quizData.get(0);
            String sheetTitle = firstRow.getTitle() != null ? firstRow.getTitle() : "Untitled";
            String type = firstRow.getType() != null ? firstRow.getType() : "";
            boolean isSurveySheet = "survey".equalsIgnoreCase(type);

            // Sanitize sheet name for Excel
            String sanitizedTitle =
                sheetTitle.replaceAll("[\\[\\]\\*\\?\\/\\\\:]", "_").replaceAll("\\s+", " ").trim();

            if (sanitizedTitle.isEmpty()) {
              sanitizedTitle = "Untitled";
            }

            if (sanitizedTitle.length() > 31) {
              sanitizedTitle = sanitizedTitle.substring(0, 28) + "...";
            }

            // Handle duplicate sheet names
            String sheetNameForQuiz = sanitizedTitle;
            int counter = 1;
            int maxAttempts = 100;
            while (workbook.getSheet(sheetNameForQuiz) != null && counter < maxAttempts) {
              String suffix = "(" + counter + ")";
              if (sanitizedTitle.length() + suffix.length() > 31) {
                sheetNameForQuiz = sanitizedTitle.substring(0, 31 - suffix.length()) + suffix;
              } else {
                sheetNameForQuiz = sanitizedTitle + suffix;
              }
              counter++;
            }

            if (counter >= maxAttempts) {
              sheetNameForQuiz = "Sheet_" + System.currentTimeMillis();
              logger.warn("Too many duplicate sheet names, using fallback: {}", sheetNameForQuiz);
            }

            Sheet quizSheet = workbook.createSheet(sheetNameForQuiz);

            // Get questions from the first participated row
            OverallParticipationDTO firstWithAnswers =
                quizData.stream()
                    .filter(
                        d -> d.getQuestionAnswers() != null && !d.getQuestionAnswers().isEmpty())
                    .findFirst()
                    .orElse(null);

            List<String> questionKeys = new ArrayList<>();
            Map<String, Integer> questionMarks = new LinkedHashMap<>();

            if (firstWithAnswers != null && firstWithAnswers.getQuestionAnswers() != null) {
              questionKeys = new ArrayList<>(firstWithAnswers.getQuestionAnswers().keySet());
            }

            if (firstWithAnswers != null && firstWithAnswers.getQuestionMarks() != null) {
              questionMarks = firstWithAnswers.getQuestionMarks();
            } else {
              for (String key : questionKeys) {
                questionMarks.put(key, 1);
              }
            }

            if (questionKeys.isEmpty()) {
              logger.warn("No question data available for quiz/survey: {}", sheetTitle);
            }

            // Build header row
            Row header = quizSheet.createRow(0);
            int hCol = 0;

            header.createCell(hCol++).setCellValue("Title");
            header.createCell(hCol++).setCellValue("StaffId");
            header.createCell(hCol++).setCellValue("Username");
            header.createCell(hCol++).setCellValue("Region");
            header.createCell(hCol++).setCellValue("Outlet");

            header.createCell(hCol++).setCellValue("Completion");

            if (!isSurveySheet) {
              header.createCell(hCol++).setCellValue("Quiz Open Date");
              header.createCell(hCol++).setCellValue("Quiz Open Time");
            } else {
              header.createCell(hCol++).setCellValue("Survey Open Date");
              header.createCell(hCol++).setCellValue("Survey Open Time");
            }

            header.createCell(hCol++).setCellValue("Agent Open Date");
            header.createCell(hCol++).setCellValue("Agent Open Time");
            header.createCell(hCol++).setCellValue("Agent Submission Date");
            header.createCell(hCol++).setCellValue("Agent Submission Time");
            header.createCell(hCol++).setCellValue("Time Taken");

            if (!isSurveySheet) {
              header.createCell(hCol++).setCellValue("Score");
              header.createCell(hCol++).setCellValue("MaxScore");
              header.createCell(hCol++).setCellValue("Percentage");
              header.createCell(hCol++).setCellValue("Result");
            } else {
              header.createCell(hCol++).setCellValue("Participated");
              header.createCell(hCol++).setCellValue("Result");
            }

            // Q columns for both quiz and survey
            for (int i = 0; i < questionKeys.size(); i++) {
              if (!isSurveySheet) {
                Integer marks = questionMarks.getOrDefault(questionKeys.get(i), 0);
                header.createCell(hCol++).setCellValue("Q" + (i + 1) + "/" + marks);
              } else {
                header.createCell(hCol++).setCellValue("Q" + (i + 1));
              }
            }

            // Selected Answer columns for survey only
            if (isSurveySheet) {
              for (int i = 0; i < questionKeys.size(); i++) {
                header.createCell(hCol++).setCellValue("Selected Answer " + (i + 1));
              }
            }

            // Remember the column index of first empty column
            int firstEmptyCol = hCol;

            // Add 3 empty columns before Question Reference
            for (int i = 0; i < 3; i++) {
              header.createCell(hCol++).setCellValue("");
            }

            // Question Reference header - two columns
            header.createCell(hCol++).setCellValue("Q No");
            header.createCell(hCol++).setCellValue("Question");

            int totalCols = hCol;

            // Write data rows
            int rowIdx = 1;
            for (OverallParticipationDTO d : quizData) {
              Row row = quizSheet.createRow(rowIdx++);
              int col = 0;

              row.createCell(col++).setCellValue(d.getTitle() != null ? d.getTitle() : "N/A");
              row.createCell(col++).setCellValue(d.getStaffId() != null ? d.getStaffId() : "N/A");
              row.createCell(col++).setCellValue(d.getUsername() != null ? d.getUsername() : "N/A");
              row.createCell(col++).setCellValue(d.getRegion() != null ? d.getRegion() : "N/A");
              row.createCell(col++).setCellValue(d.getOutlet() != null ? d.getOutlet() : "N/A");

              row.createCell(col++)
                  .setCellValue(d.getCompletion() != null ? d.getCompletion() : false);

              row.createCell(col++).setCellValue(formatDate(d.getQuizOpenTime()));
              row.createCell(col++).setCellValue(formatTime(d.getQuizOpenTime()));

              row.createCell(col++).setCellValue(formatDate(d.getAgentOpenTime()));
              row.createCell(col++).setCellValue(formatTime(d.getAgentOpenTime()));

              row.createCell(col++).setCellValue(formatDate(d.getAgentSubmissionTime()));
              row.createCell(col++).setCellValue(formatTime(d.getAgentSubmissionTime()));

              row.createCell(col++)
                  .setCellValue(
                      calculateDuration(d.getAgentOpenTime(), d.getAgentSubmissionTime()));

              if (!isSurveySheet) {
                if (d.getScore() != null) {
                  row.createCell(col).setCellValue(d.getScore());
                } else {
                  row.createCell(col).setCellValue(0);
                }
                col++;

                if (d.getMaxScore() != null) {
                  row.createCell(col).setCellValue(d.getMaxScore());
                } else {
                  row.createCell(col).setCellValue(0);
                }
                col++;

                if (d.getPercentage() != null) {
                  row.createCell(col).setCellValue(d.getPercentage() / 100.0);
                } else {
                  row.createCell(col).setCellValue(0.0);
                }
                row.getCell(col).setCellStyle(percentStyle);
                col++;
              } else {
                row.createCell(col++).setCellValue(d.isParticipated());
              }

              row.createCell(col++).setCellValue(d.getResult() != null ? d.getResult() : "N/A");

              // Get this row's questions
              List<String> thisRowKeys =
                  d.getQuestionAnswers() != null
                      ? new ArrayList<>(d.getQuestionAnswers().keySet())
                      : new ArrayList<>();

              // Q columns for both quiz and survey
              for (int i = 0; i < questionKeys.size(); i++) {
                if (i < thisRowKeys.size()) {
                  String questionKey = thisRowKeys.get(i);
                  if (!isSurveySheet) {
                    String userAnswer =
                        d.getQuestionAnswers() != null
                            ? d.getQuestionAnswers().get(questionKey)
                            : "";
                    String correctAnswer =
                        d.getCorrectAnswers() != null ? d.getCorrectAnswers().get(questionKey) : "";
                    Integer totalMarks =
                        d.getQuestionMarks() != null
                            ? d.getQuestionMarks().getOrDefault(questionKey, 0)
                            : 0;

                    int obtainedMarks = 0;
                    if (userAnswer != null
                        && correctAnswer != null
                        && userAnswer.trim().equalsIgnoreCase(correctAnswer.trim())) {
                      obtainedMarks = totalMarks;
                    }

                    row.createCell(col++).setCellValue(obtainedMarks);
                  } else {
                    // Survey - show question title in Q column
                    row.createCell(col++).setCellValue(questionKey);
                  }
                } else {
                  row.createCell(col++).setCellValue("");
                }
              }

              // Selected Answer columns for survey only
              if (isSurveySheet) {
                for (int i = 0; i < questionKeys.size(); i++) {
                  if (i < thisRowKeys.size()) {
                    String questionKey = thisRowKeys.get(i);
                    String userAnswer =
                        d.getQuestionAnswers() != null
                            ? d.getQuestionAnswers().get(questionKey)
                            : "";
                    row.createCell(col++).setCellValue(userAnswer);
                  } else {
                    row.createCell(col++).setCellValue("");
                  }
                }
              }

              // Add 3 empty columns
              for (int i = 0; i < 3; i++) {
                row.createCell(col++).setCellValue("");
              }

              // Leave Q No and Question empty for data rows
              row.createCell(col++).setCellValue("");
              row.createCell(col++).setCellValue("");
            }

            // Add question reference rows AFTER all data rows
            for (int i = 0; i < questionKeys.size(); i++) {
              Row refRow = quizSheet.createRow(rowIdx++);
              int col = 0;

              // Leave all main columns empty
              for (int j = 0; j < firstEmptyCol; j++) {
                refRow.createCell(col++).setCellValue("");
              }

              // Add 3 empty columns
              for (int j = 0; j < 3; j++) {
                refRow.createCell(col++).setCellValue("");
              }

              // Add Q No and Question
              refRow.createCell(col++).setCellValue("Q" + (i + 1));
              refRow.createCell(col++).setCellValue(questionKeys.get(i));
            }

            // Auto-size all columns except the empty ones
            for (int i = 0; i < firstEmptyCol; i++) {
              quizSheet.autoSizeColumn(i);
            }

            // Set fixed width for 3 empty columns
            for (int i = firstEmptyCol; i < firstEmptyCol + 3; i++) {
              quizSheet.setColumnWidth(i, 3000);
            }

            // Auto-size the Q No and Question columns
            quizSheet.autoSizeColumn(firstEmptyCol + 3);
            quizSheet.autoSizeColumn(firstEmptyCol + 4);
          }
        }

        // ---------------------------------------------------------------
        // QUIZ mode
        // ---------------------------------------------------------------
        else if (isQuiz) {

          @SuppressWarnings("unchecked")
          List<ParticipationStatusDTO> data = (List<ParticipationStatusDTO>) list;

          Set<String> questionSet = new LinkedHashSet<>();
          Map<String, Integer> questionMarksMap = new LinkedHashMap<>();
          for (ParticipationStatusDTO d : data) {
            if (d.getQuestion() != null) {
              questionSet.add(d.getQuestion());
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

          for (int i = 0; i < questionList.size(); i++) {
            String q = questionList.get(i);
            Integer marks = questionMarksMap.getOrDefault(q, 0);
            header.createCell(hCol++).setCellValue("Q" + (i + 1) + "/" + marks);
          }

          // Remember the column index of first empty column
          int firstEmptyCol = hCol;

          // Add 3 empty columns before Question Reference
          for (int i = 0; i < 3; i++) {
            header.createCell(hCol++).setCellValue("");
          }

          // Question Reference header - two columns
          header.createCell(hCol++).setCellValue("Q No");
          header.createCell(hCol++).setCellValue("Question");

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
            row.createCell(col++)
                .setCellValue(first.getStaffId() != null ? first.getStaffId() : "");
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

            for (String q : questionList) {

              ParticipationStatusDTO qRow = byQuestion.get(q);

              Integer marks = 0;

              if (qRow != null
                  && qRow.getAgentAnswer() != null
                  && qRow.getCorrectAnswer() != null
                  && qRow.getAgentAnswer()
                      .trim()
                      .equalsIgnoreCase(qRow.getCorrectAnswer().trim())) {

                marks = qRow.getMarks() != null ? qRow.getMarks() : 1;
              }

              row.createCell(col++).setCellValue(marks);
            }

            // Add 3 empty columns
            for (int i = 0; i < 3; i++) {
              row.createCell(col++).setCellValue("");
            }

            // Empty Q No and Question for data rows
            row.createCell(col++).setCellValue("");
            row.createCell(col++).setCellValue("");
          }

          // Add question reference rows after all data
          for (int i = 0; i < questionList.size(); i++) {
            Row refRow = sheet.createRow(rowIdx++);
            int col = 0;

            for (int j = 0; j < firstEmptyCol; j++) {
              refRow.createCell(col++).setCellValue("");
            }

            for (int j = 0; j < 3; j++) {
              refRow.createCell(col++).setCellValue("");
            }

            refRow.createCell(col++).setCellValue("Q" + (i + 1));
            refRow.createCell(col++).setCellValue(questionList.get(i));
          }

          for (int i = 0; i < firstEmptyCol; i++) {
            sheet.autoSizeColumn(i);
          }

          for (int i = firstEmptyCol; i < firstEmptyCol + 3; i++) {
            sheet.setColumnWidth(i, 3000);
          }

          sheet.autoSizeColumn(firstEmptyCol + 3);
          sheet.autoSizeColumn(firstEmptyCol + 4);
        }
        // ---------------------------------------------------------------
        // SURVEY mode
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

          // Q columns
          for (int i = 0; i < questionList.size(); i++) {
            header.createCell(hCol++).setCellValue("Q" + (i + 1));
          }

          // Selected Answer columns
          for (int i = 0; i < questionList.size(); i++) {
            header.createCell(hCol++).setCellValue("Selected Answer " + (i + 1));
          }

          // Remember the column index of first empty column
          int firstEmptyCol = hCol;

          // Add 3 empty columns before Question Reference
          for (int i = 0; i < 3; i++) {
            header.createCell(hCol++).setCellValue("");
          }

          // Question Reference header - two columns
          header.createCell(hCol++).setCellValue("Q No");
          header.createCell(hCol++).setCellValue("Question");

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
            row.createCell(col++)
                .setCellValue(first.getStaffId() != null ? first.getStaffId() : "");
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

            // Q columns - show question titles
            for (String q : questionList) {
              row.createCell(col++).setCellValue(q);
            }

            // Selected Answers - show agent answers
            for (String q : questionList) {
              ParticipationStatusDTO qRow = byQuestion.get(q);
              String ans =
                  qRow != null && qRow.getAgentAnswer() != null ? qRow.getAgentAnswer() : "";
              row.createCell(col++).setCellValue(ans);
            }

            // Add 3 empty columns
            for (int i = 0; i < 3; i++) {
              row.createCell(col++).setCellValue("");
            }

            // Leave Q No and Question empty for data rows
            row.createCell(col++).setCellValue("");
            row.createCell(col++).setCellValue("");
          }

          // Add question reference rows after all data
          for (int i = 0; i < questionList.size(); i++) {
            Row refRow = sheet.createRow(rowIdx++);
            int col = 0;

            for (int j = 0; j < firstEmptyCol; j++) {
              refRow.createCell(col++).setCellValue("");
            }

            for (int j = 0; j < 3; j++) {
              refRow.createCell(col++).setCellValue("");
            }

            refRow.createCell(col++).setCellValue("Q" + (i + 1));
            refRow.createCell(col++).setCellValue(questionList.get(i));
          }

          for (int i = 0; i < firstEmptyCol; i++) {
            sheet.autoSizeColumn(i);
          }

          for (int i = firstEmptyCol; i < firstEmptyCol + 3; i++) {
            sheet.setColumnWidth(i, 3000);
          }

          sheet.autoSizeColumn(firstEmptyCol + 3);
          sheet.autoSizeColumn(firstEmptyCol + 4);
        }
        workbook.write(out);
        return new ByteArrayInputStream(out.toByteArray());

      } catch (Exception e) {
        logger.error(
            "Error generating Excel report: type={}, dataSize={}",
            overallType,
            list != null ? list.size() : 0,
            e);

        try {
          for (int i = workbook.getNumberOfSheets() - 1; i >= 0; i--) {
            workbook.removeSheetAt(i);
          }

          Sheet errorSheet = workbook.createSheet("Error");
          Row headerRow = errorSheet.createRow(0);
          headerRow.createCell(0).setCellValue("Error generating report");

          Row messageRow = errorSheet.createRow(1);
          messageRow
              .createCell(0)
              .setCellValue(e.getMessage() != null ? e.getMessage() : "Unknown error occurred");

          Row suggestionRow = errorSheet.createRow(2);
          suggestionRow.createCell(0).setCellValue("Please try again or contact support");

          errorSheet.autoSizeColumn(0);

          workbook.write(out);
          return new ByteArrayInputStream(out.toByteArray());

        } catch (Exception ex) {
          logger.error("Failed to create error sheet", ex);
          throw new RuntimeException("Failed to generate Excel report", ex);
        }
      }
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
