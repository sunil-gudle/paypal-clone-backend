package com.dailyCode.paypal_user.common.ratelimit;

import com.dailyCode.paypal_user.auth.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Use authenticated user email as key, fall back to IP
        String key = resolveKey(request);

        if (!rateLimitService.isAllowed(key)) {
            long retryAfter = rateLimitService.getTtl(key);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setHeader("X-RateLimit-Remaining", "0");

            objectMapper.writeValue(response.getWriter(), Map.of(
                    "status", "ERROR",
                    "message", "Too many requests. Please try again in " + retryAfter + " seconds."
            ));
            return;
        }

        long remaining = rateLimitService.getRemaining(key);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtUtil.isTokenValid(token)) {
                    return "user:" + jwtUtil.extractEmail(token);
                }
            } catch (Exception ignored) {}
        }
        // Fall back to IP address
        String ip = request.getHeader("X-Forwarded-For");
        return "ip:" + (StringUtils.hasText(ip) ? ip.split(",")[0].trim() : request.getRemoteAddr());
    }
}
