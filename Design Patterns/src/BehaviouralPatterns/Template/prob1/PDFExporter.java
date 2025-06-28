package BehaviouralPatterns.Template.prob1;

class PDFExporter extends DocumentExporter {
    protected void formatContent(String content) {
        System.out.println("Formatting content as PDF...");
    }

    protected String getFormatName() {
        return "PDF";
    }
}
