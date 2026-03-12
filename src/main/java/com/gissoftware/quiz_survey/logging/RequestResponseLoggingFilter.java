package com.gissoftware.quiz_survey.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    long start = System.currentTimeMillis();
    String method = request.getMethod();
    String uri = request.getRequestURI();
    String ip = getClientIp(request);

    try {

      filterChain.doFilter(wrappedRequest, wrappedResponse);

      long duration = System.currentTimeMillis() - start;

      String requestBody =
          new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);

      String responseBody =
          new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);

      log.info(
          """
          ================= REQUEST =================
          time      : {}
          ip        : {}
          method    : {}
          uri       : {}
          body      : {}
          ===========================================
          """,
          Instant.now(),
          ip,
          method,
          uri,
          requestBody);

      log.info(
          """
          ================= RESPONSE ================
          method    : {}
          uri       : {}
          status    : {}
          duration  : {} ms
          response  : {}
          ===========================================
          """,
          method,
          uri,
          wrappedResponse.getStatus(),
          duration,
          responseBody);

    } catch (Exception ex) {

      String requestBody =
          new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);

      log.error(
          """
          ================= ERROR ===================
          time      : {}
          method    : {}
          uri       : {}
          request   : {}
          message   : {}
          ===========================================
          """,
          Instant.now(),
          method,
          uri,
          requestBody,
          ex.getMessage(),
          ex);

      throw ex;

    } finally {
      wrappedResponse.copyBodyToResponse();
    }
  }

  private String getClientIp(HttpServletRequest request) {
    String xf = request.getHeader("X-Forwarded-For");
    return (xf != null && !xf.isBlank()) ? xf.split(",")[0] : request.getRemoteAddr();
  }
}
