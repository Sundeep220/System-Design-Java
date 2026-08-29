package com.flowforge.flowforge.interceptor;

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

    private static final int MAX_REQUESTS_PER_WINDOW = 50;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final Map<String, ClientWindow> clients = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String clientIp = request.getRemoteAddr();
        ClientWindow window = clients.compute(clientIp, (ip, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new ClientWindow(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int currentCount = window.count.get();
        int remaining = Math.max(0, MAX_REQUESTS_PER_WINDOW - currentCount);

        response.setIntHeader("X-RateLimit-Limit", MAX_REQUESTS_PER_WINDOW);
        response.setIntHeader("X-RateLimit-Remaining", remaining);

        if (currentCount > MAX_REQUESTS_PER_WINDOW) {
            long elapsedMs = System.currentTimeMillis() - window.windowStart;
            long retryAfterSeconds = Math.max(1, (WINDOW_MS - elapsedMs) / 1_000);

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
