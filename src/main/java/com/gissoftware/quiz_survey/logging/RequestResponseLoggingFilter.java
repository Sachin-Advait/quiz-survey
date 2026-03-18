package com.gissoftware.quiz_survey.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

  private static final int MAX_PAYLOAD_LENGTH = 5000;

  private static final Set<String> SENSITIVE_FIELDS =
          Set.of("password", "token", "authorization", "secret");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {

    String uri = request.getRequestURI();
    String upgrade = request.getHeader("Upgrade");

    // Skip WebSocket & streaming endpoints
    return (upgrade != null && upgrade.equalsIgnoreCase("websocket"))
            || uri.contains("/ws")
            || uri.contains("/websocket")
            || uri.contains("/sockjs")
            || uri.contains("/actuator");
  }

  @Override
  protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

    // Wrap only if it's NOT async/streaming
    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    long start = System.currentTimeMillis();

    String method = request.getMethod();
    String uri = request.getRequestURI();
    String ip = getClientIp(request);

    try {

      filterChain.doFilter(wrappedRequest, wrappedResponse);

      long duration = System.currentTimeMillis() - start;

      String requestBody = getPayload(wrappedRequest.getContentAsByteArray());
      String responseBody = getPayload(wrappedResponse.getContentAsByteArray());

      requestBody = maskSensitiveData(requestBody);
      responseBody = maskSensitiveData(responseBody);

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

      String requestBody = getPayload(wrappedRequest.getContentAsByteArray());

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
              maskSensitiveData(requestBody),
              ex.getMessage(),
              ex);

      throw ex;

    } finally {
      wrappedResponse.copyBodyToResponse();
    }
  }

  private String getPayload(byte[] content) {
    if (content == null || content.length == 0) return "";

    String payload = new String(content, StandardCharsets.UTF_8);

    if (payload.length() > MAX_PAYLOAD_LENGTH) {
      return payload.substring(0, MAX_PAYLOAD_LENGTH) + "...[TRUNCATED]";
    }
    return payload;
  }

  private String maskSensitiveData(String body) {
    if (body == null || body.isBlank()) return body;

    String masked = body;

    for (String field : SENSITIVE_FIELDS) {
      masked =
              masked.replaceAll(
                      "(?i)(\"" + field + "\"\\s*:\\s*\")([^\"]+)(\")", "$1****$3");
    }
    return masked;
  }

  private String getClientIp(HttpServletRequest request) {
    String xf = request.getHeader("X-Forwarded-For");
    return (xf != null && !xf.isBlank()) ? xf.split(",")[0] : request.getRemoteAddr();
  }
}