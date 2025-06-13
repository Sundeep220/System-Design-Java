package OnlineLearningPlatform.service.impl;

import OnlineLearningPlatform.entity.Enrollment;
import OnlineLearningPlatform.repo.EnrollmentRepository;
import OnlineLearningPlatform.service.ReportFormatter;
import OnlineLearningPlatform.service.ReportService;

import java.util.List;

public class ReportServiceImpl implements ReportService {
    private final EnrollmentRepository enrollmentRepository;
    private final ReportFormatter formatter;

    public ReportServiceImpl(EnrollmentRepository enrollmentRepository, ReportFormatter formatter) {
        this.enrollmentRepository = enrollmentRepository;
        this.formatter = formatter;
    }

    public String generateReport() {
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        return formatter.format(enrollments);
    }

}
