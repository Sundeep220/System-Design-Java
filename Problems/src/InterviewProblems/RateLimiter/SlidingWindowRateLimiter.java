package InterviewProblems.RateLimiter;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowRateLimiter implements RateLimitStrategy {

    private static class UserWindow {

        Deque<Long> requestTimes =
                new ArrayDeque<>();
    }

    private final int maxRequests;
    private final long windowSizeMillis;
    private final Clock clock;

    private final Map<String, UserWindow> userWindows = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int maxRequests, long windowSizeMillis, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String userId) {
        UserWindow window = userWindows.computeIfAbsent(userId, id -> new UserWindow());

        synchronized (window) {
            long currentTime = clock.millis();

            long windowStart = currentTime - windowSizeMillis;

            // Remove requests that are outside the sliding window.
            while (!window.requestTimes.isEmpty() && window.requestTimes.peekFirst() < windowStart) {
                window.requestTimes.pollFirst();
            }

            if (window.requestTimes.size() >= maxRequests) {
                return false;
            }

            window.requestTimes.addLast(currentTime);
            return true;
        }
    }
}