package com.gissoftware.quiz_survey.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class RefererValidationFilter implements Filter {

    private static final List<String> ALLOWED_REFERERS =
            List.of(
                    "https://quiz-survey.onrender.com",
                    "https://185.177.116.176",
                    "http://localhost:3000",
                    "https://omandigitalservices.online",
                    "http://quiz-backend-route-omantel-sip.apps.ocpprod01.otg.om");

    private static final String ALLOW_OMANTEL_PREFIX = "https://omantelsip.omantel.om/";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();

        if (uri.startsWith("/api/user/ws") || uri.startsWith("/api/user/sse")) {
            chain.doFilter(request, response);
            return;
        }

        String referer = req.getHeader("Referer");
        boolean allowed = false;

        if (referer != null) {
            allowed = ALLOWED_REFERERS.stream().anyMatch(referer::startsWith);
            if (!allowed && referer.startsWith(ALLOW_OMANTEL_PREFIX)) {
                allowed = true;
            }
        }

        if (!allowed) {
            String reason =
                    referer == null ? "Missing Referer header" : "Invalid Referer header: " + referer;

            System.err.println("[RefererValidationFilter] BLOCKED - " + reason + " | URI: " + uri);

            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(403);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\": \"403 Forbidden\", \"reason\": \"" + reason + "\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
