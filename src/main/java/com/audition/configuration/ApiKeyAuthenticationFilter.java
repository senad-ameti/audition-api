package com.audition.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authentication filter that validates API key for secured actuator endpoints. Checks the X-API-Key header against the
 * configured API key and sets authentication context.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private final SecurityProperties securityProperties;

    /**
     * Filters incoming requests to apply API key authentication for secured actuator endpoints. Checks the X-API-Key
     * header and sets the security context if the key is valid. For invalid or missing keys, clears the security
     * context, resulting in a 401 response.
     *
     * @param request     the HTTP servlet request
     * @param response    the HTTP servlet response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest request,
        @NonNull final HttpServletResponse response,
        @NonNull final FilterChain filterChain) throws ServletException, IOException {

        final String requestPath = request.getRequestURI();

        // Only apply API key authentication to actuator endpoints (excluding health and info)
        if (requestPath.startsWith("/actuator/")
            && !requestPath.equals("/actuator/health")
            && !requestPath.equals("/actuator/info")) {

            final String apiKey = request.getHeader(API_KEY_HEADER);

            if (apiKey != null && apiKey.equals(securityProperties.getActuatorApiKey())) {
                // Valid API key - set authentication
                final UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken("actuator-user", null, java.util.Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // Invalid or missing API key - authentication will remain null, causing 401
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
