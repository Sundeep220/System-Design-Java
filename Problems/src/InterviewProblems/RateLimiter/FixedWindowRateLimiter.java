package InterviewProblems.RateLimiter;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * We inject Clock instead of directly calling System.currentTimeMillis()
 * so tests can control time deterministically.
 *
 * In production:
 *     Clock.systemUTC()
 *
 * In tests:
 *     We can provide a fixed or custom Clock and simulate time passing
 *     without actually waiting.
 *
 * Directly using System.currentTimeMillis() is also perfectly valid
 * for a production rate limiter, but it makes time-dependent tests harder.
 */

public class FixedWindowRateLimiter implements RateLimitStrategy {

    private static class Window {
        int requestCount;
        long windowStartTime;

        Window(long windowStartTime) {
            this.windowStartTime = windowStartTime;
        }
    }

    private final int maxRequests;
    private final long windowSizeMillis;
    private final Clock clock;

    private final Map<String, Window> userWindows = new ConcurrentHashMap<>();

    public FixedWindowRateLimiter(int maxRequests, long windowSizeMillis, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String userId) {
        Window window = userWindows.computeIfAbsent(userId, id -> new Window(currentTime()));

        // First request from this user.
        // Another way of above line
//        Window window = userWindows.get(userId);
//        if (window == null) {
//            window = new Window(currentTime);
//            userWindows.put(userId, window);
//        }

        // Only one thread can update a particular user's window at a time.
        synchronized (window) {
            long currentTime = currentTime();
            // The old window has expired.
            if (currentTime - window.windowStartTime >= windowSizeMillis) {

                window.windowStartTime = currentTime;
                window.requestCount = 0;
            }

            // Limit reached.
            if (window.requestCount >= maxRequests) {
                return false;
            }

            window.requestCount++;

            return true;
        }
    }

    private long currentTime() {
        return clock.millis();
    }
}