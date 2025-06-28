package BehaviouralPatterns.Template.prob1;

class CSVExporter extends DocumentExporter {
    protected void formatContent(String content) {
        System.out.println("Formatting content as CSV...");
    }

    protected String getFormatName() {
        return "CSV";
    }

    // Enable compression using hook
    protected boolean shouldCompress() {
        return true;
    }
}

