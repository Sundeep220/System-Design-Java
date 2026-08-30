package com.flowforge.flowforge.interceptor;

import com.flowforge.flowforge.config.FlowForgeProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final int maxRequests;
    private final long windowMs;
    private final Map<String, ClientWindow> clients = new ConcurrentHashMap<>();

    public RateLimitInterceptor(FlowForgeProperties properties) {
        this.maxRequests = properties.rateLimit().maxRequests();
        this.windowMs = properties.rateLimit().windowSeconds() * 1_000L;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String clientIp = request.getRemoteAddr();
        ClientWindow window = clients.compute(clientIp, (ip, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > windowMs) {
                return new ClientWindow(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int currentCount = window.count.get();
        int remaining = Math.max(0, maxRequests - currentCount);

        response.setIntHeader("X-RateLimit-Limit", maxRequests);
        response.setIntHeader("X-RateLimit-Remaining", remaining);

        if (currentCount > maxRequests) {
            long elapsedMs = System.currentTimeMillis() - window.windowStart;
            long retryAfterSeconds = Math.max(1, (windowMs - elapsedMs) / 1_000);

            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                      "status": 429,
                      "error": "Too Many Requests",
                      "errorCode": "RATE_LIMIT_EXCEEDED",
                      "message": "You have exceeded the rate limit. Try again in %d seconds."
                    }
                    """.formatted(retryAfterSeconds));
            return false;
        }

        return true;
    }

    private static class ClientWindow {
        final long windowStart;
        final AtomicInteger count;

        ClientWindow(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
