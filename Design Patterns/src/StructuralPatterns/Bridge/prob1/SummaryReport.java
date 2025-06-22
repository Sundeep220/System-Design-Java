package StructuralPatterns.Bridge.prob1;

public class SummaryReport extends Report {
    public SummaryReport(ReportFormatter formatter) {
        super(formatter);
    }

    @Override
    public void display() {
        String title = "Monthly Sales Summary";
        String content = "Total: $50,000";
        formatter.generate(title, content);
    }
}
