package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.OverallParticipationDTO;
import com.gissoftware.quiz_survey.dto.ParticipationStatusDTO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
              "Participated",
              "Score",
              "MaxScore",
              "Percentage",
              "Result"
            };
      } else if (isSurvey) {
        columns =
            new String[] {"StaffId", "Username", "Region", "Outlet", "Participated", "Result"};
      } else {
        columns =
            new String[] {
              "StaffId",
              "Username",
              "Region",
              "Outlet",
              "Participated",
              "Score",
              "MaxScore",
              "Percentage",
              "Result"
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
          row.createCell(0).setCellValue(d.getTitle());
          row.createCell(1).setCellValue(d.getType());
          row.createCell(2).setCellValue(d.getStaffId());
          row.createCell(3).setCellValue(d.getUsername());
          row.createCell(4).setCellValue(d.isParticipated());

          if (d.getScore() != null) row.createCell(5).setCellValue(d.getScore());
          if (d.getMaxScore() != null) row.createCell(6).setCellValue(d.getMaxScore());
          if (d.getPercentage() != null) {
            row.createCell(7).setCellValue(d.getPercentage() / 100);
            row.getCell(7).setCellStyle(percentStyle);
          }

          row.createCell(8).setCellValue(d.getResult());

        } else {
          ParticipationStatusDTO d = (ParticipationStatusDTO) obj;
          row.createCell(0).setCellValue(d.getStaffId());
          row.createCell(1).setCellValue(d.getUsername());
          row.createCell(2).setCellValue(d.getRegion());
          row.createCell(3).setCellValue(d.getOutlet());
          row.createCell(4).setCellValue(d.isParticipated());

          int col = 5;
          if (!isSurvey) {
            if (d.getScore() != null) row.createCell(col).setCellValue(d.getScore());
            col++;

            if (d.getMaxScore() != null) row.createCell(col).setCellValue(d.getMaxScore());
            col++;

            if (d.getPercentage() != null) {
              row.createCell(col).setCellValue(d.getPercentage() / 100);
              row.getCell(col).setCellStyle(percentStyle);
            }
            col++;
          }

          row.createCell(col).setCellValue(d.getResult());
        }
      }
      for (int i = 0; i < columns.length; i++) {
        sheet.autoSizeColumn(i);
      }
      workbook.write(out);
      return new ByteArrayInputStream(out.toByteArray());
    }
  }
}
