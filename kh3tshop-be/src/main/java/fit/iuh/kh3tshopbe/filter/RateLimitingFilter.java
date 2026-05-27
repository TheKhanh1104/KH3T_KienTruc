package fit.iuh.kh3tshopbe.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import fit.iuh.kh3tshopbe.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:";

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Value("${app.rate-limit.max-requests:30}")
    private long maxRequests;

    @Value("${app.rate-limit.window-seconds:60}")
    private long windowSeconds;

    public RateLimitingFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.equals("/")
                || path.startsWith("/index.html")
                || path.startsWith("/assets/")
                || path.startsWith("/static/")
                || path.startsWith("/favicon")
                || path.startsWith("/kh3tshop-fe/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientKey = resolveClientKey(request);
        String redisKey = RATE_LIMIT_KEY_PREFIX + clientKey;

        boolean allowed = rateLimitService.isAllowed(redisKey, maxRequests, Duration.ofSeconds(windowSeconds));
        if (!allowed) {
            long retryAfterSeconds = rateLimitService.getRetryAfterSeconds(redisKey);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(retryAfterSeconds, 1L)));
            response.setContentType("application/json");

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Too many requests. Please try again later.");
            body.put("limit", maxRequests);
            body.put("windowSeconds", windowSeconds);
            body.put("retryAfterSeconds", retryAfterSeconds);
            body.put("client", clientKey);

            objectMapper.writeValue(response.getWriter(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "user:" + authentication.getName();
        }

        return "ip:" + resolveClientIp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}