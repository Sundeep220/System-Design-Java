package Basics.StackOverflow.entity;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Comment {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    private final int id;
    private final String content;
    private final User author;
    private final Post post;
    private final LocalDateTime createdAt;

    public Comment(User author, String content, Post post) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.author = author;
        this.content = content;
        this.post = post;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public User getAuthor() {
        return author;
    }

    public Post getPost() {
        return post;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}