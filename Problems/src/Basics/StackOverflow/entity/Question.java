package Basics.StackOverflow.entity;

import java.util.*;

public class Question extends Post {

    private String title;
    private final List<Tag> tags;
    private final List<Answer> answers;

    private static final int MAX_TAGS = 10;

    public Question(User author, String title, String content) {
        super(author, content);
        this.title = title;
        this.tags = new ArrayList<>();
        this.answers = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }

    public void addTag(Tag tag) {
        if (tags.size() >= MAX_TAGS) {
            throw new RuntimeException("Maximum 10 tags allowed.");
        }
        tags.add(tag);
    }

    public List<Tag> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void addAnswer(Answer answer) {
        answers.add(answer);
    }

    public List<Answer> getAnswers() {
        return Collections.unmodifiableList(answers);
    }
}