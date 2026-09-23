package com.gissoftware.quiz_survey.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final String secretKey;
  private final String adminSecretKey;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public JwtAuthenticationFilter(
      @Value("${jwt.secret}") String secretKey,
      @Value("${jwt.admin-secret}") String adminSecretKey) {

    this.secretKey = secretKey;
    this.adminSecretKey = adminSecretKey;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String method = request.getMethod();
    String uri = request.getRequestURI();

    String authorization = request.getHeader("Authorization");

    // ==========================================
    // Authorization header
    // ==========================================

    if (authorization == null) {

      unauthorized(response, "Authorization token is required");
      return;
    }

    if (!authorization.startsWith("Bearer ")) {

      unauthorized(response, "Authorization token is required");
      return;
    }

    String token = authorization.substring(7).trim();

    if (token.isEmpty()) {

      unauthorized(response, "Authorization token is required");
      return;
    }

    JwtResult result = null;

    try {

      result = validateToken(token, secretKey);

    } catch (Exception e) {

      System.out.println("NORMAL JWT VALIDATION: FAILED");
      System.out.println("Exception type: " + e.getClass().getName());
      System.out.println("Exception msg : " + e.getMessage());
    }

    String validatedBy = "NORMAL";

    // ==========================================
    // Try admin secret
    // ==========================================

    if (result == null) {

      System.out.println("------------------------------------------");
      System.out.println("Trying ADMIN JWT secret...");

      try {

        result = validateToken(token, adminSecretKey);
        validatedBy = "ADMIN";

      } catch (Exception e) {

        System.out.println("ADMIN JWT VALIDATION: FAILED");
        System.out.println("Exception type: " + e.getClass().getName());
        System.out.println("Exception msg : " + e.getMessage());
      }
    }

    // ==========================================
    // Both failed
    // ==========================================

    if (result == null) {

      SecurityContextHolder.clearContext();

      unauthorized(response, "Invalid or expired token");
      return;
    }

    // ==========================================
    // Get subject
    // ==========================================

    Object subjectObject = result.claims.get("sub");

    String userId = subjectObject != null ? subjectObject.toString() : null;

    if (userId == null || userId.isBlank()) {

      SecurityContextHolder.clearContext();

      unauthorized(response, "Invalid token: subject is missing");
      return;
    }

    // ==========================================
    // Create Spring Authentication
    // ==========================================

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(
            userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

    SecurityContextHolder.getContext().setAuthentication(authentication);

    filterChain.doFilter(request, response);
  }

  // ============================================================
  // JWT validation using raw HMAC-SHA512
  // ============================================================

  private JwtResult validateToken(String token, String secret) throws Exception {

    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("JWT secret is null or empty");
    }

    String[] parts = token.split("\\.");

    if (parts.length != 3) {
      throw new IllegalArgumentException("JWT must contain exactly 3 parts");
    }

    String headerPart = parts[0];
    String payloadPart = parts[1];
    String signaturePart = parts[2];

    // ==========================================
    // Decode header
    // ==========================================

    byte[] headerBytes = Base64.getUrlDecoder().decode(headerPart);

    String headerJson = new String(headerBytes, StandardCharsets.UTF_8);

    Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);

    Object algorithmObject = header.get("alg");

    String algorithm = algorithmObject != null ? algorithmObject.toString() : null;

    if (!"HS512".equals(algorithm)) {

      throw new IllegalArgumentException("Unsupported JWT algorithm: " + algorithm);
    }

    // ==========================================
    // Calculate HMAC-SHA512
    // ==========================================

    String signingInput = headerPart + "." + payloadPart;

    Mac mac = Mac.getInstance("HmacSHA512");

    SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");

    mac.init(key);

    byte[] calculatedSignature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));

    // ==========================================
    // Decode JWT signature
    // ==========================================

    byte[] actualSignature = Base64.getUrlDecoder().decode(signaturePart);

    // ==========================================
    // Compare signatures safely
    // ==========================================

    if (!MessageDigest.isEqual(calculatedSignature, actualSignature)) {

      throw new SecurityException("JWT signature does not match");
    }

    // ==========================================
    // Decode payload
    // ==========================================

    byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadPart);

    String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);

    Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

    // ==========================================
    // Check expiration
    // ==========================================

    Object expObject = claims.get("exp");

    if (expObject != null) {

      long expiration;

      if (expObject instanceof Number) {

        expiration = ((Number) expObject).longValue();

      } else {

        expiration = Long.parseLong(expObject.toString());
      }

      long currentTime = System.currentTimeMillis() / 1000;

      if (currentTime >= expiration) {

        throw new SecurityException("JWT has expired");
      }
    }

    return new JwtResult(claims);
  }

  // ============================================================
  // JWT result
  // ============================================================

  private void unauthorized(HttpServletResponse response, String message) throws IOException {

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    response.getWriter().write("{\"message\":\"" + message + "\"}");
  }

  // ============================================================
  // HTTP 401
  // ============================================================

  private record JwtResult(Map<String, Object> claims) {}
}
