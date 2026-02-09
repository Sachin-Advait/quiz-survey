package com.gissoftware.quiz_survey.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    long start = System.currentTimeMillis();
    String method = request.getMethod();
    String uri = request.getRequestURI();
    String ip = getClientIp(request);

    log.info("➡️ REQUEST | time={} | ip={} | method={} | uri={}", Instant.now(), ip, method, uri);

    try {
      filterChain.doFilter(request, response);
    } catch (Exception ex) {
      log.error("❌ ERROR | method={} | uri={} | message={}", method, uri, ex.getMessage(), ex);
      throw ex;
    } finally {
      long duration = System.currentTimeMillis() - start;
      log.info(
          "⬅️ RESPONSE | method={} | uri={} | status={} | duration={}ms",
          method,
          uri,
          response.getStatus(),
          duration);
    }
  }

  private String getClientIp(HttpServletRequest request) {
    String xf = request.getHeader("X-Forwarded-For");
    return (xf != null && !xf.isBlank()) ? xf.split(",")[0] : request.getRemoteAddr();
  }
}
