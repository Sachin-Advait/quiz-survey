package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.dto.OfferViewReportDTO;
import com.gissoftware.quiz_survey.service.OfferReportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/offers")
@RequiredArgsConstructor
public class OfferReportController {

  private final OfferReportService offerReportService;

  @GetMapping("/view-report")
  public ResponseEntity<ApiResponseDTO<List<OfferViewReportDTO>>> getAllOfferViewReport() {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true, "All offer view reports fetched", offerReportService.getAllOfferViewReport()));
  }

  @GetMapping("/{offerId}/view-report")
  public ResponseEntity<ApiResponseDTO<List<OfferViewReportDTO>>> getOfferViewReport(
      @PathVariable String offerId) {

    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true, "Offer view report fetched", offerReportService.getOfferViewReport(offerId)));
  }

  @GetMapping("/view-report/excel")
  public ResponseEntity<byte[]> downloadAllOfferViewExcel() {

    byte[] excel = offerReportService.getAllOfferViewReportExcel();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDispositionFormData("attachment", "offer-view-report.xlsx");

    return ResponseEntity.ok().headers(headers).body(excel);
  }
}
