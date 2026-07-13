package InterviewProblems.StackOverflow.entity;

import Basics.StackOverflow.entity.Post;
import Basics.StackOverflow.entity.User;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class Comment {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    private final int id;
    private final String content;
    private final Basics.StackOverflow.entity.User author;
    private final Basics.StackOverflow.entity.Post post;
    private final LocalDateTime createdAt;

    public Comment(Basics.StackOverflow.entity.User author, String content, Basics.StackOverflow.entity.Post post) {
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