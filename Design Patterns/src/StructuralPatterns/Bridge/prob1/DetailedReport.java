package StructuralPatterns.Bridge.prob1;

public class DetailedReport extends Report {
    public DetailedReport(ReportFormatter formatter) {
        super(formatter);
    }

    @Override
    public void display() {
        String title = "Monthly Sales - Detailed";
        String content = "Item1: $10,000\nItem2: $15,000\nItem3: $25,000";
        formatter.generate(title, content);
    }
}
