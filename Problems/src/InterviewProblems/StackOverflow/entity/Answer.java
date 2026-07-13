package InterviewProblems.StackOverflow.entity;

import Basics.StackOverflow.entity.Post;
import Basics.StackOverflow.entity.Question;
import Basics.StackOverflow.entity.User;

public class Answer extends Post {

    private final Question question;

    public Answer(User author, String content, Question question) {
        super(author, content);
        this.question = question;
    }

    public Question getQuestion() {
        return question;
    }
}