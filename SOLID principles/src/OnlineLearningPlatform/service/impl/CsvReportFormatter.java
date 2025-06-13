package OnlineLearningPlatform.service.impl;

import OnlineLearningPlatform.entity.Enrollment;
import OnlineLearningPlatform.service.ReportFormatter;

import java.util.List;
import java.util.stream.Collectors;

public class CsvReportFormatter implements ReportFormatter {

    @Override
    public String format(List<Enrollment> enrollments) {
        String header = "StudentName,CourseTitle,EnrolledAt";
        return enrollments.stream()
                .map(e -> e.getStudent().getName() + "," +
                        e.getCourse().getTitle() + "," +
                        e.getEnrolledAt())
                .collect(Collectors.joining("\n", header + "\n", ""));
    }
}
