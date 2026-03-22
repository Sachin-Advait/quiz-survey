package com.gissoftware.quiz_survey.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class HostValidationFilter implements Filter {

    private static final List<String> ALLOWED_HOSTS =
            List.of(
                    "localhost:3000",
                    "localhost:8082",
                    "localhost:8443",
                    "quiz-survey.onrender.com",
                    "185.177.116.176",
                    "omandigitalservices.online",
                    "quiz-backend-route-omantel-sip.apps.ocpprod01.otg.om",
                    "omantelsip.omantel.om");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();

        // ✅ ALWAYS allow WebSocket (CRITICAL)
        if (uri.startsWith("/api/user/ws")) {
            chain.doFilter(request, response);
            return;
        }

        String host = req.getHeader("Host");

        if (host != null && !ALLOWED_HOSTS.contains(host)) {
            String reason = "Invalid Host header: " + host;

            System.err.println("[HostValidationFilter] BLOCKED - " + reason + " | URI: " + uri);

            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(400);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\": \"400 Bad Request\", \"reason\": \"" + reason + "\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
