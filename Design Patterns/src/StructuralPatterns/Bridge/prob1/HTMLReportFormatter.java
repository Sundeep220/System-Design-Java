package StructuralPatterns.Bridge.prob1;


public class HTMLReportFormatter implements ReportFormatter {
    @Override
    public void generate(String title, String content) {
        System.out.println("<html><body>");
        System.out.println("<h1>" + title + "</h1>");
        System.out.println("<p>" + content + "</p>");
        System.out.println("</body></html>");
    }
}
