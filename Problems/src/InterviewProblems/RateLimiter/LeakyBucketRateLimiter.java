package InterviewProblems.RateLimiter;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LeakyBucketRateLimiter implements RateLimitStrategy {

    private static class Bucket {
        double waterLevel;
        long lastLeakTime;
        Bucket(long currentTime) {
            this.lastLeakTime = currentTime;
        }
    }

    private final int capacity;
    private final double leakRatePerMillis;
    private final Clock clock;

    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    public LeakyBucketRateLimiter(int capacity, double leakRatePerSecond, Clock clock) {
        this.capacity = capacity;
        this.leakRatePerMillis = leakRatePerSecond / 1000.0;
        this.clock = clock;
    }

    @Override
    public boolean allowRequest(String userId) {
        Bucket bucket = userBuckets.computeIfAbsent(userId, id -> new Bucket(currentTime()));

        synchronized (bucket) {
            long currentTime = currentTime();
            leak(bucket, currentTime);
            if (bucket.waterLevel >= capacity) {
                return false;
            }
            bucket.waterLevel++;
            return true;
        }
    }

    private void leak(Bucket bucket, long currentTime) {
        long elapsedTime = currentTime - bucket.lastLeakTime;
        double leaked = elapsedTime * leakRatePerMillis;
        bucket.waterLevel = Math.max(0, bucket.waterLevel - leaked);
        bucket.lastLeakTime = currentTime;
    }

    private long currentTime() {
        return clock.millis();
    }
}