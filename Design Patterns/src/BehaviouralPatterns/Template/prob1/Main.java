package BehaviouralPatterns.Template.prob1;

public class Main {
        public static void main(String[] args) {
            String content = "Employee data, confidential";

            DocumentExporter pdfExporter = new PDFExporter();
            System.out.println("Exporting PDF...");
            pdfExporter.export(content);

            System.out.println("\nExporting DOCX...");
            DocumentExporter docxExporter = new DOCXExporter();
            docxExporter.export(content);

            System.out.println("\nExporting CSV...");
            DocumentExporter csvExporter = new CSVExporter();
            csvExporter.export(content);
        }
}
