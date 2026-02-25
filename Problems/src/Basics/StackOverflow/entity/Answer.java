package Basics.StackOverflow.entity;

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