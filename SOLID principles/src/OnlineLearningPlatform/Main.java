package OnlineLearningPlatform;

import OnlineLearningPlatform.entity.Course;
import OnlineLearningPlatform.entity.Student;
import OnlineLearningPlatform.entity.Teacher;
import OnlineLearningPlatform.repo.CourseRepository;
import OnlineLearningPlatform.repo.EnrollmentRepository;
import OnlineLearningPlatform.service.EnrollmentService;
import OnlineLearningPlatform.service.Notifier;
import OnlineLearningPlatform.service.ReportFormatter;
import OnlineLearningPlatform.service.ReportService;
import OnlineLearningPlatform.service.impl.*;

public class Main {
    public static void main(String[] args) {
        // Setup
        CourseRepository courseRepository = new CourseRepository();
        EnrollmentRepository enrollmentRepository = new EnrollmentRepository();

        // Teachers and Courses
        Teacher teacher = new Teacher("T1", "Dr. Sharma");
        Course course = new Course("C1", "Java Backend Development", "Learn Spring Boot & SOLID", teacher);
        courseRepository.save(course);

        // Students
        Student student = new Student("S1", "Alice", "alice@example.com", "9999999999");

        // Notifier - You can switch to SMSNotifier here
        Notifier notifier = new EmailNotifier();

        // Enrollment Service
        EnrollmentService enrollmentService = new EnrollmentServiceImpl(notifier, courseRepository, enrollmentRepository);

        // Enroll student
        enrollmentService.enroll(student, course);


//        ReportFormatter formatter = new CsvReportFormatter();
        ReportFormatter formatter = new JsonReportFormatter();

        ReportService reportService = new ReportServiceImpl(enrollmentRepository, formatter);
        System.out.println("===== REPORT =====");
        System.out.println(reportService.generateReport());
    }
}
