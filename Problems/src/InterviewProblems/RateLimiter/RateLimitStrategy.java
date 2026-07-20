package InterviewProblems.RateLimiter;

public interface RateLimitStrategy {
    boolean allowRequest(String userId);
}