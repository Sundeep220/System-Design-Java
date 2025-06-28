package BehaviouralPatterns.Template.prob1;

abstract class DocumentExporter {
    // Template method
    public final void export(String content) {
        openFile();
        formatContent(content);
        saveFile();
        if (shouldCompress()) {
            compressFile();
        }
    }

    private void openFile() {
        System.out.println("Opening " + getFormatName() + " file...");
    }

    private void saveFile() {
        System.out.println("Saving file...");
    }

    // Steps to be implemented by subclasses
    protected abstract void formatContent(String content);
    protected abstract String getFormatName();

    // Hook method (optional)
    protected boolean shouldCompress() {
        return false;
    }

    protected void compressFile() {
        System.out.println("Compressing " + getFormatName() + " file...");
    }
}

