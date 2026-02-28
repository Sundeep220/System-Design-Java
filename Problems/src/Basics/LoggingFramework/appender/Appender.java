package Basics.LoggingFramework.appender;

import Basics.LoggingFramework.enitity.LogRecord;

public interface Appender {
    void append(LogRecord record);
}