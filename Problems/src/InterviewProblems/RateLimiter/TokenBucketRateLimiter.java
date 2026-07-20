package InterviewProblems.RateLimiter;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBucketRateLimiter implements RateLimitStrategy {

    private static class Bucket {
        double tokens;
        long lastRefillTime;

        Bucket(double tokens, long lastRefillTime) {
            this.tokens = tokens;
            this.lastRefillTime = lastRefillTime;
        }
    }

    private final int capacity;
    private final double refillRatePerMillis;
    private final Clock clock;

    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int capacity, double refillRatePerSecond, Clock clock) {
        this.capacity = capacity;
        this.refillRatePerMillis = refillRatePerSecond / 1000.0;
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String userId) {
        Bucket bucket = userBuckets.computeIfAbsent(userId, id -> new Bucket(capacity, currentTime()));
        synchronized (bucket) {
            long currentTime = currentTime();
            refill(bucket, currentTime);
            if (bucket.tokens < 1) {
                return false;
            }
            bucket.tokens--;
            return true;
        }
    }

    private void refill(Bucket bucket, long currentTime) {
        long elapsedTime = currentTime - bucket.lastRefillTime;
        double tokensToAdd = elapsedTime * refillRatePerMillis;
        bucket.tokens = Math.min(capacity, bucket.tokens + tokensToAdd);
        bucket.lastRefillTime = currentTime;
    }

    private long currentTime() {
        return clock.millis();
    }
}