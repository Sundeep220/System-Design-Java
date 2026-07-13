package InterviewProblems.StackOverflow;

import Basics.StackOverflow.entity.Answer;
import Basics.StackOverflow.entity.Question;
import Basics.StackOverflow.entity.Tag;
import Basics.StackOverflow.entity.User;
import Basics.StackOverflow.enums.VoteType;
import Basics.StackOverflow.repo.QuestionRepository;
import Basics.StackOverflow.repo.UserRepository;
import Basics.StackOverflow.service.QuestionService;
import Basics.StackOverflow.service.ReputationService;
import Basics.StackOverflow.service.VoteService;
import Basics.StackOverflow.service.strategy.InMemorySearchService;
import Basics.StackOverflow.service.strategy.SearchService;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        // --- Repositories ---
        UserRepository userRepository = new UserRepository();
        QuestionRepository questionRepository = new QuestionRepository();

        // --- Services ---
        ReputationService reputationService = new ReputationService();
        VoteService voteService = new VoteService(reputationService);
        SearchService searchService = new InMemorySearchService(questionRepository);
        QuestionService questionService = new QuestionService(questionRepository, searchService);

        // --- Create Users ---
        User alice = new User("Alice");
        User bob = new User("Bob");
        User charlie = new User("Charlie");

        userRepository.save(alice);
        userRepository.save(bob);
        userRepository.save(charlie);

        // --- Create Question ---
        Question question = questionService.createQuestion(
                alice,
                "What is polymorphism in Java?",
                "Can someone explain polymorphism with examples?"
        );

        // --- Add Tags ---
        question.addTag(new Tag("java"));
        question.addTag(new Tag("oop"));

        // --- Add Answer ---
        Answer answer = questionService.addAnswer(
                bob,
                question,
                "Polymorphism allows objects to be treated as instances of their parent class."
        );

        // --- Voting ---
        voteService.castVote(charlie, question, VoteType.UPVOTE);
        voteService.castVote(charlie, answer, VoteType.UPVOTE);

        // --- Print Stats ---
        System.out.println("Question Upvotes: " + question.getUpvoteCount());
        System.out.println("Answer Upvotes: " + answer.getUpvoteCount());

        System.out.println("Alice Reputation: " + alice.getReputation());
        System.out.println("Bob Reputation: " + bob.getReputation());

        // --- Search ---
        List<Question> results = questionService.search("polymorphism", 0, 10);

        System.out.println("\nSearch Results:");
        for (Question q : results) {
            System.out.println("Title: " + q.getTitle());
            System.out.println("Upvotes: " + q.getUpvoteCount());
        }
    }
}