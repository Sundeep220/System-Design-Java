package OnlineLearningPlatform.service.impl;

import OnlineLearningPlatform.entity.Enrollment;
import OnlineLearningPlatform.service.ReportFormatter;

import java.util.List;

public class JsonReportFormatter implements ReportFormatter {

    @Override
    public String format(List<Enrollment> enrollments) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (Enrollment e : enrollments) {
            sb.append("  {\n")
                    .append("    \"student\": \"").append(e.getStudent().getName()).append("\",\n")
                    .append("    \"course\": \"").append(e.getCourse().getTitle()).append("\",\n")
                    .append("    \"enrolledAt\": \"").append(e.getEnrolledAt()).append("\"\n")
                    .append("  },\n");
        }
        if (!enrollments.isEmpty()) {
            sb.setLength(sb.length() - 2); // remove last comma
        }
        sb.append("\n]");
        return sb.toString();
    }
}