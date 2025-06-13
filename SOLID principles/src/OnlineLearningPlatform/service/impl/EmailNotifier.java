package OnlineLearningPlatform.service.impl;

import OnlineLearningPlatform.entity.Student;
import OnlineLearningPlatform.service.Notifier;

public class EmailNotifier implements Notifier {
    @Override
    public void notify(Student student, String message) {
        System.out.println("Email sent to " + student.getEmail() + ": " + message);
    }
}
