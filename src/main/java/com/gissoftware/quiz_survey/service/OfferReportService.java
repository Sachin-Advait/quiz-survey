package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.OfferViewReportDTO;
import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.model.OfferView;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.OfferRepository;
import com.gissoftware.quiz_survey.repository.OfferViewRepository;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferReportService {

  private final OfferRepository offerRepository;
  private final OfferViewRepository offerViewRepository;
  private final UserRepository userRepository;

  public List<OfferViewReportDTO> getOfferViewReport(String offerId) {

    OfferModel offer =
        offerRepository
            .findById(offerId)
            .orElseThrow(() -> new RuntimeException("Offer not found"));

    List<OfferView> views = offerViewRepository.findByOfferId(offerId);

    Map<String, OfferView> viewedMap =
        views.stream().collect(Collectors.toMap(OfferView::getUserId, v -> v));

    List<UserModel> users = userRepository.findAll();

    return users.stream()
        .map(
            user -> {
              OfferView view = viewedMap.get(user.getId());

              boolean viewed = view != null;

              return new OfferViewReportDTO(
                  offer.getId(),
                  offer.getTitle(),
                  user.getId(),
                  user.getUsername(),
                  user.getStaffId(),
                  viewed,
                  viewed ? view.getViewedAt() : null // ✅ FIX
                  );
            })
        .sorted((a, b) -> Boolean.compare(b.isViewed(), a.isViewed()))
        .toList();
  }

  public List<OfferViewReportDTO> getAllOfferViewReport() {

    List<OfferModel> offers = offerRepository.findAll();
    List<UserModel> users = userRepository.findAll();
    List<OfferView> views = offerViewRepository.findAll();

    Map<String, OfferView> viewMap =
        views.stream()
            .collect(
                Collectors.toMap(v -> v.getOfferId() + "_" + v.getUserId(), v -> v, (a, b) -> a));

    List<OfferViewReportDTO> result = new ArrayList<>();

    for (OfferModel offer : offers) {
      for (UserModel user : users) {
        OfferView view = viewMap.get(offer.getId() + "_" + user.getId());
        boolean viewed = view != null;

        result.add(
            new OfferViewReportDTO(
                offer.getId(),
                offer.getTitle(),
                user.getId(),
                user.getUsername(),
                user.getStaffId(),
                viewed,
                viewed ? view.getViewedAt() : null));
      }
    }

    return result.stream()
        .sorted(
            Comparator.comparing(
                    OfferViewReportDTO::getOfferTitle, Comparator.nullsLast(String::compareTo))
                .thenComparing(OfferViewReportDTO::isViewed, Comparator.reverseOrder()))
        .toList();
  }

    public byte[] getAllOfferViewReportExcel() {

        List<OfferViewReportDTO> data = getAllOfferViewReport();

        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

        DateTimeFormatter dateTimeFormatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Offer View Report");

            // HEADER
            Row header = sheet.createRow(0);
            String[] columns = {
                    "Offer Title",
                    "Staff ID",
                    "Learner",
                    "Viewed",
                    "Viewed Date",
                    "Viewed Timestamp"
            };

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // DATA
            int rowIdx = 1;
            for (OfferViewReportDTO dto : data) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(dto.getOfferTitle());
                row.createCell(1).setCellValue(dto.getStaffId());
                row.createCell(2).setCellValue(dto.getUserName());
                row.createCell(3).setCellValue(dto.isViewed() ? "Yes" : "No");

                row.createCell(4)
                        .setCellValue(
                                dto.getViewedAt() != null ? dateFormatter.format(dto.getViewedAt()) : "");

                row.createCell(5)
                        .setCellValue(
                                dto.getViewedAt() != null ? dateTimeFormatter.format(dto.getViewedAt()) : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate offer view Excel", e);
        }
    }

}
