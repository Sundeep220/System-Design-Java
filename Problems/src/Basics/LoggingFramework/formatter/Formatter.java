package Basics.LoggingFramework.formatter;

import Basics.LoggingFramework.enitity.LogRecord;

public interface Formatter {
    String format(LogRecord record);
}