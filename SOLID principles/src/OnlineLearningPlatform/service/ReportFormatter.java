package OnlineLearningPlatform.service;

import OnlineLearningPlatform.entity.Enrollment;

import java.util.List;

public interface ReportFormatter {
    String format(List<Enrollment> enrollments);
}