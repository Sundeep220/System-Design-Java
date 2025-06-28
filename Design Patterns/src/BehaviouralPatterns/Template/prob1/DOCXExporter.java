package BehaviouralPatterns.Template.prob1;

class DOCXExporter extends DocumentExporter {
    protected void formatContent(String content) {
        System.out.println("Formatting content as DOCX...");
    }

    protected String getFormatName() {
        return "DOCX";
    }
}
