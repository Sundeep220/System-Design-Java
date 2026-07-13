package InterviewProblems.StackOverflow.service.strategy;

import Basics.StackOverflow.entity.Question;

import java.util.List;

public interface SearchService {
    List<Question> search(String keyword, int page, int size);
}