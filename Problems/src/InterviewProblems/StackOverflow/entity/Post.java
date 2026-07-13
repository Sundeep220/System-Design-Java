package InterviewProblems.StackOverflow.entity;

import Basics.StackOverflow.entity.Comment;
import Basics.StackOverflow.entity.User;
import Basics.StackOverflow.entity.Vote;
import Basics.StackOverflow.enums.VoteType;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Post {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    private final int id;
    private final User author;
    private String content;
    private final LocalDateTime createdAt;
    private int version;

    private final List<Comment> comments;
    private final List<Vote> votes;

    private final AtomicInteger upvoteCount;
    private final AtomicInteger downvoteCount;

    protected Post(User author, String content) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.author = author;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.version = 0;

        this.comments = new ArrayList<>();
        this.votes = new ArrayList<>();
        this.upvoteCount = new AtomicInteger(0);
        this.downvoteCount = new AtomicInteger(0);
    }

    public int getId() {
        return id;
    }

    public User getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public int getVersion() {
        return version;
    }

    public int getUpvoteCount() {
        return upvoteCount.get();
    }

    public int getDownvoteCount() {
        return downvoteCount.get();
    }

    public void updateContent(String newContent, int expectedVersion) {
        if (this.version != expectedVersion) {
            throw new RuntimeException("Version mismatch. Optimistic locking failed.");
        }
        this.content = newContent;
        this.version++;
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void applyVote(Vote vote) {
        votes.add(vote);

        if (vote.getVoteType() == VoteType.UPVOTE) {
            upvoteCount.incrementAndGet();
        } else {
            downvoteCount.incrementAndGet();
        }
    }

    public void removeVote(Vote vote) {
        votes.remove(vote);

        if (vote.getVoteType() == VoteType.UPVOTE) {
            upvoteCount.decrementAndGet();
        } else {
            downvoteCount.decrementAndGet();
        }
    }
}