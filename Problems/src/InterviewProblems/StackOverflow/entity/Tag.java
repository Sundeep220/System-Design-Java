package InterviewProblems.StackOverflow.entity;

import java.util.concurrent.atomic.AtomicInteger;

public class Tag {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    private final int id;
    private final String name;

    public Tag(String name) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.name = name.toLowerCase();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}