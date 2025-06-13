package OnlineLearningPlatform.service;

import OnlineLearningPlatform.entity.Course;
import OnlineLearningPlatform.entity.Student;

public interface EnrollmentService {
    void enroll(Student student, Course course);
}
