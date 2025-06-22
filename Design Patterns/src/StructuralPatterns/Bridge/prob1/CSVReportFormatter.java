package StructuralPatterns.Bridge.prob1;

public class CSVReportFormatter implements ReportFormatter {
    @Override
    public void generate(String title, String content) {
        System.out.println("Title,Content");
        System.out.println(title + "," + content);
    }
}

