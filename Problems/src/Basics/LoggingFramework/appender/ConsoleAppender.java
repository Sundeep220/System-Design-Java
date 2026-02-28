package Basics.LoggingFramework.appender;

import Basics.LoggingFramework.enitity.LogRecord;
import Basics.LoggingFramework.formatter.Formatter;

public class ConsoleAppender implements Appender {

    private final Formatter formatter;

    public ConsoleAppender(Formatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void append(LogRecord record) {
        String formattedMessage = formatter.format(record);
        System.out.println(formattedMessage);
    }
}