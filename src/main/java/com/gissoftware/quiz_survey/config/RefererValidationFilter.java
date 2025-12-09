package com.gissoftware.quiz_survey.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public class RefererValidationFilter implements Filter {

    private static final List<String> ALLOWED_REFERERS = List.of(
            "https://quiz-survey.onrender.com",
            "https://185.177.116.176",
            "http://localhost:3000",
            "http://quiz-backend-route-omantel-sip.apps.ocpprod01.otg.om"
    );

    private static final String ALLOW_OMANTEL_PREFIX = "https://omantelsip.omantel.om/";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String referer = req.getHeader("Referer");

        boolean allowed = false;

        if (referer != null) {
            // check hardcoded allowed referers
            allowed = ALLOWED_REFERERS.stream().anyMatch(referer::startsWith);

            // check any URL starting with the omantelsip prefix
            if (!allowed && referer.startsWith(ALLOW_OMANTEL_PREFIX)) {
                allowed = true;
            }
        }

        if (!allowed) {
            ((HttpServletResponse) response).sendError(403, "Invalid Referer header");
            return;
        }

        chain.doFilter(request, response);
    }
}
