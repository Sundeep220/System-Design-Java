package OnlineLearningPlatform.entity;

import java.time.LocalDateTime;

public class Enrollment {
    private final Student student;
    private final Course course;
    private final LocalDateTime enrolledAt;

    public Enrollment(Student student, Course course, LocalDateTime enrolledAt) {
        this.student = student;
        this.course = course;
        this.enrolledAt = enrolledAt;
    }

    public Student getStudent() {
        return student;
    }

    public Course getCourse() {
        return course;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    // Getters
}
