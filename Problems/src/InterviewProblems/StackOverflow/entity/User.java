package InterviewProblems.StackOverflow.entity;

import java.util.concurrent.atomic.AtomicInteger;

public class User {
    private final AtomicInteger x = new AtomicInteger(0);
    private final int id;
    private String name;
    private int reputation;

    public User(String name) {
        this.id = x.incrementAndGet();
        this.name = name;
        this.reputation = 0; // default reputation
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getReputation() {
        return reputation;
    }

    public void increaseReputation(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        this.reputation += points;
    }

    public void decreaseReputation(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        this.reputation -= points;
    }
}