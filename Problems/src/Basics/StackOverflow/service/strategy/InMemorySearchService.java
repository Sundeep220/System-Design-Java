package Basics.StackOverflow.service.strategy;

import Basics.StackOverflow.entity.Question;
import Basics.StackOverflow.repo.QuestionRepository;

import java.util.List;
import java.util.stream.Collectors;

public class InMemorySearchService implements SearchService {

    private final QuestionRepository questionRepository;

    public InMemorySearchService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public List<Question> search(String keyword, int page, int size) {
        List<Question> filtered = questionRepository.findAll()
                .stream()
                .filter(q -> q.getTitle().contains(keyword) ||
                        q.getContent().contains(keyword))
                .collect(Collectors.toList());

        int start = page * size;
        int end = Math.min(start + size, filtered.size());

        if (start >= filtered.size()) return List.of();

        return filtered.subList(start, end);
    }
}