package Basics.StackOverflow.service;

import Basics.StackOverflow.entity.Post;
import Basics.StackOverflow.entity.User;
import Basics.StackOverflow.entity.Vote;
import Basics.StackOverflow.enums.VoteType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VoteService {

    private final Map<String, Vote> voteStore = new ConcurrentHashMap<>();
    private final ReputationService reputationService;

    public VoteService(ReputationService reputationService) {
        this.reputationService = reputationService;
    }

    private String generateKey(int userId, int postId) {
        return userId + "_" + postId;
    }

    public void castVote(User user, Post post, VoteType voteType) {

        String key = generateKey(user.getId(), post.getId());

        if (voteStore.containsKey(key)) {
            throw new RuntimeException("User has already voted on this post.");
        }

        Vote vote = new Vote(user, voteType, post);

        voteStore.put(key, vote);
        post.applyVote(vote);

        reputationService.handleVote(post.getAuthor(), voteType);
    }

    public void removeVote(User user, Post post) {

        String key = generateKey(user.getId(), post.getId());
        Vote vote = voteStore.remove(key);

        if (vote == null) {
            throw new RuntimeException("No vote to remove.");
        }

        post.removeVote(vote);
        reputationService.handleVoteRemoval(post.getAuthor(), vote.getVoteType());
    }
}