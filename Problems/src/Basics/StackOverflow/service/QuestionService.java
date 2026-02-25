package Basics.StackOverflow.service;

import Basics.StackOverflow.entity.Answer;
import Basics.StackOverflow.entity.Question;
import Basics.StackOverflow.entity.User;
import Basics.StackOverflow.repo.QuestionRepository;
import Basics.StackOverflow.service.strategy.SearchService;

import java.util.List;
import java.util.stream.Collectors;

public class QuestionService {

    private final QuestionRepository questionRepository;
    private final SearchService searchService;

    public QuestionService(QuestionRepository questionRepository, SearchService searchService) {
        this.questionRepository = questionRepository;
        this.searchService = searchService;
    }

    public Question createQuestion(User author, String title, String content) {
        Question question = new Question(author, title, content);
        questionRepository.save(question);
        return question;
    }

    public Answer addAnswer(User author, Question question, String content) {
        Answer answer = new Answer(author, content, question);
        question.addAnswer(answer);
        return answer;
    }

//    public List<Question> searchByKeyword(String keyword) {
//        return questionRepository.findAll()
//                .stream()
//                .filter(q -> q.getTitle().contains(keyword) ||
//                        q.getContent().contains(keyword))
//                .collect(Collectors.toList());
//    }

    public List<Question> search(String keyword, int page, int size) {
        return searchService.search(keyword, page, size);
    }
}