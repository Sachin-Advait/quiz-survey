package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.OverallParticipationDTO;
import com.gissoftware.quiz_survey.dto.ParticipationStatusDTO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
      Row header = sheet.createRow(0);

      String[] columns;

      if (overall) {

        columns =
            new String[] {
              "Title",
              "Type",
              "StaffId",
              "Username",
              "Region",
              "Outlet",
              "Question",
              "Agent Answer",
              "Correct Answer",
              "Completion",
              "Quiz Open Time",
              "Agent Open Time",
              "Agent Submission Time",
              "Score",
              "MaxScore",
              "Percentage",
              "Result"
            };

      } else if (isQuiz) {

        columns =
            new String[] {
              "Title",
              "StaffId",
              "Username",
              "Region",
              "Outlet",
              "Question",
              "Agent Answer",
              "Correct Answer",
              "Completion",
              "Quiz Open Time",
              "Agent Open Time",
              "Agent Submission Time",
              "Score",
              "MaxScore",
              "Percentage",
              "Result"
            };

      } else {

        // survey
        columns =
            new String[] {
              "Title", "StaffId", "Username", "Region", "Outlet", "Participated", "Result"
            };
      }

      for (int i = 0; i < columns.length; i++) {
        header.createCell(i).setCellValue(columns[i]);
      }

      int rowIdx = 1;

      for (Object obj : list) {

        Row row = sheet.createRow(rowIdx++);

        if (overall) {

          OverallParticipationDTO d = (OverallParticipationDTO) obj;

          row.createCell(0).setCellValue(d.getTitle() != null ? d.getTitle() : "");
          row.createCell(1).setCellValue(d.getType() != null ? d.getType() : "");
          row.createCell(2).setCellValue(d.getStaffId() != null ? d.getStaffId() : "");
          row.createCell(3).setCellValue(d.getUsername() != null ? d.getUsername() : "");
          row.createCell(4).setCellValue(d.getRegion() != null ? d.getRegion() : "");
          row.createCell(5).setCellValue(d.getOutlet() != null ? d.getOutlet() : "");
          row.createCell(6).setCellValue(d.getQuestion() != null ? d.getQuestion() : "");
          row.createCell(7).setCellValue(d.getAgentAnswer() != null ? d.getAgentAnswer() : "");
          row.createCell(8).setCellValue(d.getCorrectAnswer() != null ? d.getCorrectAnswer() : "");
          row.createCell(9).setCellValue(d.getCompletion() != null ? d.getCompletion() : false);
          row.createCell(10).setCellValue(formatInstant(d.getQuizOpenTime()));
          row.createCell(11).setCellValue(formatInstant(d.getAgentOpenTime()));
          row.createCell(12).setCellValue(formatInstant(d.getAgentSubmissionTime()));

          if (d.getScore() != null) {
            row.createCell(13).setCellValue(d.getScore());
          }

          if (d.getMaxScore() != null) {
            row.createCell(14).setCellValue(d.getMaxScore());
          }

          if (d.getPercentage() != null) {
            row.createCell(15).setCellValue(d.getPercentage() / 100);
            row.getCell(15).setCellStyle(percentStyle);
          }

          row.createCell(16).setCellValue(d.getResult() != null ? d.getResult() : "");

        } else if (isQuiz) {

          ParticipationStatusDTO d = (ParticipationStatusDTO) obj;

          row.createCell(0).setCellValue(d.getTitle() != null ? d.getTitle() : "");
          row.createCell(1).setCellValue(d.getStaffId() != null ? d.getStaffId() : "");
          row.createCell(2).setCellValue(d.getUsername() != null ? d.getUsername() : "");
          row.createCell(3).setCellValue(d.getRegion() != null ? d.getRegion() : "");
          row.createCell(4).setCellValue(d.getOutlet() != null ? d.getOutlet() : "");
          row.createCell(5).setCellValue(d.getQuestion() != null ? d.getQuestion() : "");
          row.createCell(6).setCellValue(d.getAgentAnswer() != null ? d.getAgentAnswer() : "");
          row.createCell(7).setCellValue(d.getCorrectAnswer() != null ? d.getCorrectAnswer() : "");
          row.createCell(8).setCellValue(d.getCompletion() != null ? d.getCompletion() : false);
          row.createCell(9).setCellValue(formatInstant(d.getQuizOpenTime()));
          row.createCell(10).setCellValue(formatInstant(d.getAgentOpenTime()));
          row.createCell(11).setCellValue(formatInstant(d.getAgentSubmissionTime()));

          if (d.getScore() != null) {
            row.createCell(12).setCellValue(d.getScore());
          }

          if (d.getMaxScore() != null) {
            row.createCell(13).setCellValue(d.getMaxScore());
          }

          if (d.getPercentage() != null) {
            row.createCell(14).setCellValue(d.getPercentage() / 100);
            row.getCell(14).setCellStyle(percentStyle);
          }

          row.createCell(15).setCellValue(d.getResult() != null ? d.getResult() : "");

        } else {

          // survey
          ParticipationStatusDTO d = (ParticipationStatusDTO) obj;

          row.createCell(0).setCellValue(d.getTitle() != null ? d.getTitle() : "");
          row.createCell(1).setCellValue(d.getStaffId() != null ? d.getStaffId() : "");
          row.createCell(2).setCellValue(d.getUsername() != null ? d.getUsername() : "");
          row.createCell(3).setCellValue(d.getRegion() != null ? d.getRegion() : "");
          row.createCell(4).setCellValue(d.getOutlet() != null ? d.getOutlet() : "");
          row.createCell(5).setCellValue(d.isParticipated());
          row.createCell(6).setCellValue(d.getResult() != null ? d.getResult() : "");
        }
      }

      for (int i = 0; i < columns.length; i++) {
        sheet.autoSizeColumn(i);
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
}
