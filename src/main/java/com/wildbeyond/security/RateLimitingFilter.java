package com.wildbeyond.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.rate-limit.login.capacity:5}")
    private long loginCapacity;

    @Value("${app.security.rate-limit.login.refill-minutes:1}")
    private long loginRefillMinutes;

    @Value("${app.security.rate-limit.api.capacity:100}")
    private long apiCapacity;

    @Value("${app.security.rate-limit.api.refill-minutes:1}")
    private long apiRefillMinutes;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        RatePolicy policy = resolvePolicy(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = policy.key + ":" + clientIp(request);
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket(policy.capacity, policy.refillMinutes));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write("{\"error\":\"Too many requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RatePolicy resolvePolicy(HttpServletRequest request) {
        String path = request.getRequestURI();

        if ("POST".equalsIgnoreCase(request.getMethod()) && "/auth/login".equals(path)) {
            return new RatePolicy("login", loginCapacity, loginRefillMinutes);
        }

        if (path.startsWith("/api/")) {
            return new RatePolicy("api", apiCapacity, apiRefillMinutes);
        }

        return null;
    }

    private Bucket newBucket(long capacity, long refillMinutes) {
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.greedy(capacity, Duration.ofMinutes(refillMinutes)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class RatePolicy {
        private final String key;
        private final long capacity;
        private final long refillMinutes;

        private RatePolicy(String key, long capacity, long refillMinutes) {
            this.key = key;
            this.capacity = capacity;
            this.refillMinutes = refillMinutes;
        }
    }
}
