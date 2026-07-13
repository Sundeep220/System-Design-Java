package InterviewProblems.StackOverflow.entity;

import Basics.StackOverflow.entity.Post;
import Basics.StackOverflow.entity.User;
import Basics.StackOverflow.enums.VoteType;

import java.util.concurrent.atomic.AtomicInteger;

public class Vote {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    private final int id;
    private final Basics.StackOverflow.entity.User voter;
    private final VoteType voteType;
    private final Basics.StackOverflow.entity.Post post;

    public Vote(Basics.StackOverflow.entity.User voter, VoteType voteType, Basics.StackOverflow.entity.Post post) {
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