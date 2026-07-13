package InterviewProblems.StackOverflow.service;

import Basics.StackOverflow.entity.User;
import Basics.StackOverflow.enums.VoteType;

public class ReputationService {

    public void handleVote(User postAuthor, VoteType voteType) {
        if (voteType == VoteType.UPVOTE) {
            postAuthor.increaseReputation(10);
        } else {
            postAuthor.decreaseReputation(2);
        }
    }

    public void handleVoteRemoval(User postAuthor, VoteType voteType) {
        if (voteType == VoteType.UPVOTE) {
            postAuthor.decreaseReputation(10);
        } else {
            postAuthor.increaseReputation(2);
        }
    }
}