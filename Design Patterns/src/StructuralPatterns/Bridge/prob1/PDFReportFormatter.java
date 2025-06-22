package StructuralPatterns.Bridge.prob1;

public class PDFReportFormatter implements ReportFormatter {
    @Override
    public void generate(String title, String content) {
        System.out.println("[PDF Report]");
        System.out.println("Title: " + title);
        System.out.println("Content: " + content);
    }
}
