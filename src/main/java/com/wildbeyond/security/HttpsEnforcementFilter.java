package com.wildbeyond.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class HttpsEnforcementFilter extends OncePerRequestFilter {

    @Value("${app.security.require-https:false}")
    private boolean requireHttps;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!requireHttps || isSecureRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        StringBuilder target = new StringBuilder();
        target.append("https://").append(request.getServerName());

        if (request.getServerPort() > 0 && request.getServerPort() != 80 && request.getServerPort() != 443) {
            target.append(":").append(request.getServerPort());
        }

        target.append(request.getRequestURI());
        if (request.getQueryString() != null) {
            target.append("?").append(request.getQueryString());
        }

        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", target.toString());
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }

        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && "https".equalsIgnoreCase(forwardedProto);
    }
}
