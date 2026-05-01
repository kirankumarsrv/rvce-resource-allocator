package com.rvce.scas.hardening;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Applies security-related HTTP headers to every API response.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP =
            "default-src 'self'; "
                    + "script-src 'self'; "
                    + "style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data: blob:; "
                    + "font-src 'self'; "
                    + "connect-src 'self' https://scas.rvce.edu.in; "
                    + "frame-ancestors 'none'; "
                    + "object-src 'none'; "
                    + "base-uri 'self'; "
                    + "form-action 'self'";

    private static final String HSTS = "max-age=31536000; includeSubDomains";

    private static final String PERMISSIONS_POLICY =
            "camera=(self), geolocation=(self), microphone=(), payment=(), usb=()";

    /**
     * Adds hardened response headers before the request continues through the chain.
     *
     * @param request the incoming HTTP request
     * @param response the HTTP response to mutate
     * @param chain the downstream filter chain
     * @throws ServletException if the container cannot continue processing
     * @throws IOException if the response cannot be written
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy", CSP);
        response.setHeader("Strict-Transport-Security", HSTS);
        response.setHeader("X-XSS-Protection", "0");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", PERMISSIONS_POLICY);
        response.setHeader("X-Powered-By", "");
        response.setHeader("Server", "");

        chain.doFilter(request, response);
    }
}
