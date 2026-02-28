package Basics.LoggingFramework.appender;

import Basics.LoggingFramework.enitity.LogRecord;
import Basics.LoggingFramework.formatter.Formatter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileAppender implements Appender {

    private final Formatter formatter;
    private final String filePath;

    public FileAppender(String filePath, Formatter formatter) {
        this.filePath = filePath;
        this.formatter = formatter;
    }

    @Override
    public void append(LogRecord record) {
        String formattedMessage = formatter.format(record);

        try (FileWriter fw = new FileWriter(filePath, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(formattedMessage);

        } catch (IOException e) {
            // Logging must not crash application
            // Swallow or optionally print to stderr
            System.err.println("File logging failed: " + e.getMessage());
        }
    }
}