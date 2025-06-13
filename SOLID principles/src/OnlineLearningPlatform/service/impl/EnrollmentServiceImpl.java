package OnlineLearningPlatform.service.impl;

import OnlineLearningPlatform.entity.Course;
import OnlineLearningPlatform.entity.Enrollment;
import OnlineLearningPlatform.entity.Student;
import OnlineLearningPlatform.repo.CourseRepository;
import OnlineLearningPlatform.repo.EnrollmentRepository;
import OnlineLearningPlatform.service.EnrollmentService;
import OnlineLearningPlatform.service.Notifier;

import java.time.LocalDateTime;

public class EnrollmentServiceImpl implements EnrollmentService {
    private final Notifier notifier;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

   public EnrollmentServiceImpl(Notifier notifier, CourseRepository courseRepository, EnrollmentRepository enrollmentRepository) {
        this.notifier = notifier;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }


    @Override
    public void enroll(Student student, Course course) {
        Course courseToEnroll = courseRepository.findById(course.getId()).orElse(null);
        if (courseToEnroll == null) {
            throw new IllegalArgumentException("Invalid course ID: " + course.getId());
        }
        enrollmentRepository.save(new Enrollment(student, courseToEnroll, LocalDateTime.now()));
        notifier.notify(student, "You have enrolled in " + courseToEnroll.getTitle());
    }
}
