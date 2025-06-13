package OnlineLearningPlatform.service.impl;

import OnlineLearningPlatform.entity.Student;
import OnlineLearningPlatform.service.Notifier;

public class SMSNotifier implements Notifier {
    @Override
    public void notify(Student student, String message) {
        System.out.println("SMS sent to " + student.getPhone() + ": " + message);
    }
}