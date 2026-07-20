package InterviewProblems.RateLimiter;

public class RateLimiter {

    private final RateLimitStrategy strategy;

    public RateLimiter(RateLimitStrategy strategy) {
        this.strategy = strategy;
    }

    public boolean allowRequest(String userId) {
        return strategy.allowRequest(userId);
    }
}