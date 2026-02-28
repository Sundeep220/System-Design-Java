package Basics.LoggingFramework.formatter;

import Basics.LoggingFramework.enitity.LogRecord;

import java.time.format.DateTimeFormatter;

public class PlainTextFormatter implements Formatter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_INSTANT;

    @Override
    public String format(LogRecord record) {
        return String.format(
                "%s [%s] [%s] %s",
                FORMATTER.format(record.getTimestamp()),
                record.getLevel(),
                record.getThreadName(),
                record.getMessage()
        );
    }
}