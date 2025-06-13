package SRP.ReportGeneratorProb;

public class Main {
    public static void main(String[] args) {
        ReportFetcher reportFetcher = new ReportFetcher();
        ReportFormatter reportFormatter = new ReportFormatter();
        PrintReport printReport = new PrintReport();
        JsonReportFormatter jsonReportFormatter = new JsonReportFormatter();

        String report = reportFetcher.getReport();
        String formattedReport = reportFormatter.formatReport(report);
        printReport.printReport(formattedReport);
        formattedReport = jsonReportFormatter.formatReport(report);
        printReport.printReport(formattedReport);
    }
}
