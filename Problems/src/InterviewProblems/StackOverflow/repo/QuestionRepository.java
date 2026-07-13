package InterviewProblems.StackOverflow.repo;
import Basics.StackOverflow.entity.Question;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestionRepository {

    private final Map<Integer, Question> questions = new ConcurrentHashMap<>();

    public void save(Question question) {
        questions.put(question.getId(), question);
    }

    public Optional<Question> findById(int id) {
        return Optional.ofNullable(questions.get(id));
    }

    public Collection<Question> findAll() {
        return questions.values();
    }
}