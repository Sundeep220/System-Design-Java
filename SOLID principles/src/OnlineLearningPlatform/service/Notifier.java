package OnlineLearningPlatform.service;

import OnlineLearningPlatform.entity.Student;

public interface Notifier {
    void notify(Student student, String message);
}