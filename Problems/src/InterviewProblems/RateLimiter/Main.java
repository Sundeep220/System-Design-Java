package InterviewProblems.RateLimiter;

import java.time.Clock;

public class Main {

    public static void main(String[] args) {

        Clock clock = Clock.systemUTC();

        System.out.println("===== FIXED WINDOW =====");
        RateLimiter fixedWindow = new RateLimiter(new FixedWindowRateLimiter(3, 10_000, clock));
        testLimiter(fixedWindow);


        System.out.println("\n===== SLIDING WINDOW =====");
        RateLimiter slidingWindow = new RateLimiter(new SlidingWindowRateLimiter(3, 10_000, clock));
        testLimiter(slidingWindow);


        System.out.println("\n===== TOKEN BUCKET =====");
        RateLimiter tokenBucket = new RateLimiter(new TokenBucketRateLimiter(3, 1, clock));
        testLimiter(tokenBucket);


        System.out.println("\n===== LEAKY BUCKET =====");
        RateLimiter leakyBucket = new RateLimiter(new LeakyBucketRateLimiter(3, 1, clock));
        testLimiter(leakyBucket);
    }

    private static void testLimiter(RateLimiter rateLimiter) {
        for (int i = 1; i <= 5; i++) {
            boolean allowed = rateLimiter.allowRequest("user1");
            System.out.println("Request " + i + " → " + (allowed ? "ALLOWED" : "REJECTED"));
        }
    }
}