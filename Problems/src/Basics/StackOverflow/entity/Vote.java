package Basics.StackOverflow.entity;

import Basics.StackOverflow.enums.VoteType;

import java.util.concurrent.atomic.AtomicInteger;

public class Vote {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    private final int id;
    private final User voter;
    private final VoteType voteType;
    private final Post post;

    public Vote(User voter, VoteType voteType, Post post) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.voter = voter;
        this.voteType = voteType;
        this.post = post;
    }

    public int getId() {
        return id;
    }

    public User getVoter() {
        return voter;
    }

    public VoteType getVoteType() {
        return voteType;
    }

    public Post getPost() {
        return post;
    }
}