package StructuralPatterns.Bridge.prob1;

public class Main {
    public static void main(String[] args) {
        ReportFormatter pdf = new PDFReportFormatter();
        ReportFormatter html = new HTMLReportFormatter();
        ReportFormatter csv = new CSVReportFormatter();

        Report summaryPdf = new SummaryReport(pdf);
        Report detailedHtml = new DetailedReport(html);
        Report summaryCsv = new SummaryReport(csv);

        System.out.println("---- Summary PDF ----");
        summaryPdf.display();

        System.out.println("\n---- Detailed HTML ----");
        detailedHtml.display();

        System.out.println("\n---- Summary CSV ----");
        summaryCsv.display();
    }
}
